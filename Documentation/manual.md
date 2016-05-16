---
layout: page
title: User Guide
---
# Content
* TOC
{:toc}

# Getting Started
Flow uses sbt as build system. To get started, add the Bintray jcenter resolver and include a dependency to flow in your project:

~~~scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "ch.jodersky" %% "flow-core" % "@version@"
~~~

Next, you need to include flow's native library that supports communication for serial devices.

## Including Native Library
There are two options to include the native library:

1. Using a pre-packaged dependency, available only for certain OSes but easily included.

2. Including the library manually for maximum portability.

It is recommended that you use the first option for testing purposes or end-user applications. The second option is recomended for libraries, since it leaves more choice to the end-user.

### The Easy Way
In case your kernel/architecture combination is present in the "supported platforms" table in the [downloads section]({{site.url}}/downloads/), add a second dependency to your project:

~~~scala
libraryDependencies += "ch.jodersky" % "flow-native" % "@version@" % "runtime"
~~~

This will add a jar to your classpath containing native libraries for various platforms. At run-time, the correct library for the current platform is selected, extracted and loaded. This solution enables running applications seamlessly, as if they were pure JVM applications.

### Maximum Portability
Start by obtaining a copy of the native library, either by [building flow](./developer) or by [downloading]({{site.url}}/downloads/) a native archive. In order to work with this version of flow, native libraries need to be of major version @native_major@ and minor version greater or equal to @native_minor@.

Then, for every end-user application that relies on flow, manually add the native library for the current platform to the JVM's library path. This can be achieved through various ways, notably:

- Per application:

    Run your program with the command-line option ```-Djava.library.path=".:<folder containing libflow@native_major@.so>"```. E.g. ```java -Djava.library.path=".:/home/<folder containing libflow@native_major@.so>" -jar your-app.jar```

- System- or user-wide:

	Copy the native library to a place that is on the default Java library path and run your application normally. Such places usually include `/usr/lib` and `/usr/local/lib`.

---

# Communication Protocol
The following is a general guide on the usage of flow. If you prefer a complete example, check out the code contained in the [flow-samples](https://github.com/jodersky/flow/tree/v@version@/flow-samples) directory.

Flow's API follows that of an actor based system, where each actor is assigned specific functions involved in serial communication. The two main actor types are:

1. Serial "manager". The manager is a singleton actor that is instantiated once per actor system, a reference to it may be obtained with `IO(Serial)`. It is typically used to open serial ports (see following section).

2. Serial "operators". Operators are created once per open serial port and serve as an intermediate between client code and native code dealing with serial data transmission and reception. They isolate the user from threading issues and enable the reactive dispatch of incoming data. A serial operator is said to be "associated" to its underlying open serial port.

The messages understood by flow's actors are all contained in the `ch.jodersky.flow.Serial` object. They are well documented and should serve as the entry point when searching the API documentation.

## Opening a Port
A serial port is opened by sending an `Open` message to the serial manager. The response varies on the outcome of opening the underlying serial port.

1. In case of failure, the serial manager will respond with a `CommandFailed` message to the original sender. The message contains details on the reason to why the opening failed.

2. In case of success, the sender is notified with an `Opened` message. This message is sent from an operator actor, spawned by the serial manager. It is useful to capture the sender (i.e. the operator) of this message as all further communication with the newly opened port must pass through the operator.

~~~scala
import ch.jodersky.flow.{ Serial, SerialSettings, AccessDeniedException }

val port = "/dev/ttyXXX"
val settings = SerialSettings(
  baud = 115200,
  characterSize = 8,
  twoStopBits = false,
  parity = Parity.None
)

IO(Serial) ! Serial.Open(port, settings)

def receive = {
  case Serial.CommandFailed(cmd: Serial.Open, reason: AccessDeniedException) =>
    println("You're not allowed to open that port!")
  case Serial.CommandFailed(cmd: Serial.Open, reason) =>
	println("Could not open port for some other reason: " + reason.getMessage)
  case Serial.Opened(settings) => {
    val operator = sender
    //do stuff with the operator, e.g. context become opened(op)
  }
}
~~~

## Writing Data
Writing data is as simple as sending a `Write` message to an operator. The data to send is an instance of `akka.util.ByteString`:

~~~scala
operator ! Serial.Write(data)
~~~

Optionally, an acknowledgement for sent data can be requested by adding an `ack` parameter to a `Write` message. The `ack` parameter is of type `Int => Serial.Event`, i.e. a function that takes the number of actual bytes written and returns an event. Note that "bytes written" refers to bytes enqueued in a kernel buffer; no guarantees can be made on the actual transmission of the data.

~~~scala

case class MyPacketAck(wrote: Int) extends Serial.Event

operator ! Serial.Write(data, MyPacketAck(_))
operator ! Serial.Write(data, n => MyPacketAck(n))

def receive = {
  case MyPacketAck(n) => println("Wrote " + n + " bytes of data")
}

~~~

## Receiving Data
The actor that opened a serial port (referred to as the client), exclusively receives incomming messages from the operator. These messages are in the form of `akka.util.ByteString`s and wrapped in a `Received` object.

~~~scala
def receive = {
  case Serial.Received(data) => println("Received data: " + data.toString)
}
~~~

## Closing a Port
A port is closed by sending a `Close` message to its operator:
~~~scala
operator ! Serial.Close
~~~
The operator will close the underlying serial port and respond with a final `Closed` message before terminating.


## Resources and Error Handling
The operator has a deathwatch on the client actor that opened the port, this means that if the latter crashes, the operator closes the port and equally terminates, freeing any allocated resources.

The opposite is not true by default, i.e. if the operator crashes (this can happen for example on IO errors) it dies silently and the client is not informed. Therefore, it is recommended that the client keep a deathwatch on the operator.

---

# Watching Ports
As of version 2.2.0, flow can watch directories for new files. On most unix systems this can be used for watching for new serial ports in `/dev/`.
Watching happens through a message-based, publish-subscribe protocol as explained in the sections below.

## Subscribing
A client actor may watch -- i.e subscribe to notifications on -- a directory by sending a `Watch` command to the serial manager.

Should an error be encountered whilst trying to obtain the watch, the manager will respond with a `CommandFailed` message.
Otherwise, the client may be considered "subscribed" to the directory and the serial manager will thenceforth notify
the client on new files.

~~~scala
IO(Serial) ! Serial.Watch("/dev/")

def receive = {
  case Serial.CommandFailed(w: Watch, reason) =>
    println(s"Cannot obtain a watch on ${w.directory}: ${reason.getMessage}")
}

~~~

## Notifications
Whilst subscribed to a directory, a client actor is informed of any new files in said directory by receiving
`Connected` messages from the manager.

~~~scala
def receive = {
  case Serial.Connected(port) if port matches "/dev/ttyUSB\\d+" =>
    // do something with the available port, e.g.
    // IO(Serial) ! Open(port, settings)
}
~~~

## Unsubscribing
Unsubscribing from events on a directory is done by sending an `Unsubscribe` message to the serial manager.

~~~scala
IO(Serial) ! Unwatch("/dev/")
~~~

## Resource Handling
Note that the manager has a deathwatch on every subscribed client. Hence, should a client die, any underlying resources will be freed.

---

# Stream Support
Flow provides support for Akka streams and thus can be interfaced with reactive-streams. Support is implemented in a separate module, which needs to be added as a library dependency:

~~~scala
libraryDependencies += "ch.jodersky" %% "flow-stream" % "@version@"
~~~

The main entry point for serial streaming is `ch.jodersky.flow.stream.Serial`. It's API is also well documented and should serve as the starting point when searching documentation on serial streaming.

## Opening a Port
Connection is established by materializing a `Flow[ByteString, ByteString, Future[Connection]]` obtained by calling `Serial().open()`

~~~scala
val serial: Flow[ByteString, ByteString, Future[Connection]] = Serial().open("/dev/ttyUSB0", settings)

val source: Source[ByteString, _] = // some source
val sink: Sink[ByteString, _] = // some sink

source.viaMat(serial)(Keep.right).toMat(sink)(Keep.left).run() onComplete {
  case Success(connection) => // a serial connection has been established
  case Failure(error) => // connection could not be established due to error
}
~~~

The materialized future will be completed with a `Success` in case the port is opened or a `Failure` in case an error is encountered whilst opening.

## Communication
Any data pushed to the `Flow`'s inlet will be sent to the serial port and any data received by the port will be emitted by the `Flow`'s outlet.

Note that backpressure is only available for writing, to add backpressure on the receiving side a higher-level protocol needs to be implemented on top of serial communication.

## Closing a Port
The underlying serial port is closed when its materialized serial flow is closed.

## Errors and Resource Handling
Any errors described in flow-core can also be encountered in flow-streaming. When thrown, they will be wrapped as the cause of a `StreamSerialException` and cause the the serial `Flow` stage to fail.

As with flow-core, native resources are handled by underlying Akka mechanisms and any crashes in user code will automatically case the resources to be freed.
