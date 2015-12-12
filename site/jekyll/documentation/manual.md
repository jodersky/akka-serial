---
layout: page
---

# Content
* TOC
{:toc}

# Getting Started
Flow uses SBT as build system. To get started, include a dependency to flow in your project:

~~~scala
libraryDependencies += "com.github.jodersky" %% "flow" % "{{site.data.releases.current.version}}"
~~~

Next, you need to include flow's native library that supports communication for serial devices.

## Including Native Library
There are two options to include the native library:

1. Using an easy, pre-packaged dependency, avialble only for certain OSes.

2. Including the library manually for maximum portability.

It is recommended that you use the first option for testing purposes or end-user applications. The second option is recomended for libraries, since it leaves more choice to the end-user.

### The Easy Way
In case your OS/architecture combination is present in the "supported platforms" table in the downloads section, add a second dependency to your project:

~~~scala
libraryDependencies += "com.github.jodersky" % "flow-native" % "{{site.data.releases.current.version}}" % "runtime"
~~~

This will add a jar to your classpath containing native libraries for various platforms. At run time, the correct library for the current platform is selected, extracted and loaded. This solution enables running applications seamlessly, as if they were pure JVM applications.

### Maximum Portability
First, obtain a copy of the native library, either by [building flow](#building-from-source) or by [downloading]({{site.url}}/downloads) a precompiled version. In order to work with this version of flow, native libraries need to be of major version {{site.data.releases.current.native_version.major}} and minor version greater or equal to {{site.data.releases.current.native_version.minor}}. 

Second, for every end-user application that relies on flow, manually add the native library for the current platform to the JVM's library path. This can be achieved through various ways, notably:

- Per application:
  Run your program with the command-line option ```-Djava.library.path=".:<folder containing libflow{{site.data.releases.current.native_version.major}}.so>"```. E.g. ```java -Djava.library.path=".:/home/<folder containing libflow{{site.data.releases.current.native_version.major}}.so>" -jar your-app.jar```

- System- or user-wide:

	- Copy the native library to a place that is on the default Java library path and run your application normally. Such places usually include `/usr/lib` and `/usr/local/lib`.

	- Install a native package from the downloads section

---

# Communication Protocol
The following is a general guide on the usage of flow. If you prefer a complete example, check out the code contained in the [flow-samples](https://github.com/jodersky/flow/tree/master/flow-samples) directory. 

Flow's API follows that of an actor based system, where each actor is assigned specific functions involved in serial communication. The two main actor types are:

1. Serial "manager". The manager is a singleton actor that is instantiated once per actor system, a reference to it may be obtained with `IO(Serial)`. It is typically used to open serial ports (see following section).

2. Serial "operators". Operators are created once per open serial port and serve as an intermediate between client code and native code dealing with serial data transmission and reception. They isolate the user from threading issues and enable the reactive dispatch of incoming data. A serial operator is said to be "associated" to its underlying open serial port.

The messages understood by flow's actors are all contained in the `com.github.jodersky.flow.Serial` object. They are well documented and should serve as the entry point when searching the API documentation.

## Opening a Port
A serial port is opened by sending an `Open` message to the serial manager. The response varies on the outcome of opening the underlying serial port.

1. In case of failure, the serial manager will respond with a `CommandFailed` message to the original sender. The message contains details on the reason to why the opening failed.

2. In case of success, the sender is notified with an `Opened` message. This message is sent from an operator actor, spawned by the serial manager. It is useful to capture the sender (i.e. the operator) of this message as all further communication with the newly opened port must pass through the operator.

~~~scala
import com.github.jodersky.flow.{ Serial, SerialSettings, AccessDeniedException }

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

## Requirements
Flow uses Java's `WatchService`s under the hood, therefore a Java runtime of a version of at least 1.7 is required.

---

# Building from Source
A complete build of flow involves two parts

1. Building Scala sources (the front-end), resulting in a platform independent artifact (i.e. a jar file).

2. Building C sources (the back-end), yielding a native library that may only be used on systems resembling the platform for which it was compiled.

Both steps are independent, their only interaction being a header file generated by the JDK utility `javah` (see `sbt javah` for details), and may therefore be built in any order.

## Building Scala Sources
Run `sbt flow-main/packageBin` in the base directory. This simply compiles Scala sources as with any standard sbt project and packages the resulting class files in a jar.

## Building Native Sources
The back-end is managed by GNU Autotools and all relevant files are contained in `flow-native`.

{::options parse_block_html="true" /}
<aside class="notice">
### Aside: Autotools Introduction
Autotools is a suite of programs constituting a sort of "meta-build system". It is used to generate a platform-independent build script known as `./configure`, which, when run, will analyze the current system (search for a C compiler, required libraries etc) and produce a `Makefile`. The makefile in turn is system-specific and can be used to create the final binary. In summary the build process is as follows:

1. Autotools (specifically the program `autoreconf`) generates `./configure`, this happens on the developer's machine
2. `./configure` is run on the host computer
3. `make` is run to produce a binary, also on the host computer

In a typical, source-controlled repository, only a bootstrapping script that calls Autotools is checked into version control. However, source *releases* include the generated `./configure` script. An end-user then downloads a source release and only has to run `./configure && make`.

However, since flow does currently not provide source releases (not to be confused with source repository or Git tags), the developer's machine is the same as the host machine and so the bootstrapping process always needs to be performed.
</aside>

### Build Process

Several steps are involved in producing the native library:

1. Bootstrap the build (run this once, if `./configure` does not exist).

    1. Check availability of dependencies: autotools and libtool (on Debian-based systems run `apt-get install build-essential autoconf automake libtool`)
    2. Run `./bootstrap`

2. Compile
   
    1. Check availability of dependencies: C compiler and JDK (1.8 or above)
    2. Run `./configure && make`.
       *Note: should you encounter an error about a missing "jni.h" file, try setting the JAVA_HOME environment variable to point to base path of your JDK installation.*

3. Install

    The native library is now ready and can be:

	- copied to a local directory: `DESTDIR=$(pwd)/<directory> make install`

    - installed system-wide: `make install`

    - put into a "fat" jar, useful for dependency management with SBT (see next section)

### Creating a Fat Jar
The native library produced in the previous step may be bundled into a "fat" jar so that it can be included in SBT projects through its regular dependency mechanisms. In this process, SBT basically acts as a wrapper script around Autotools, calling the native build process and packaging generated libraries. Running `sbt flow-native/packageBin` in the base directory produces the fat jar in `flow-native/target`.

Note: an important feature of fat jars is to include native libraries for several platforms. To copy binaries compiled on other platforms to the fat jar, place them in a subfolder of `flow-native/lib_native`. The subfolder should have the name `com/github/jodersky/flow/native/$(arch)-$(kernel)`, where `arch` and `kernel` are, respectively, the lower-case values returned by `uname -m` and `uname -s`.

### Note About Versioning
The project and package versions follow a [semantic](http://semver.org/) pattern: `M.m.p`, where

- `M` is the major version, representing backwards incompatible changes

- `m` is the minor version, indicating backwards compatible changes such as new feature additions

- `p` is the patch number, representing internal modifications such as bug-fixes
 
Usually (following most Linux distribution's conventions), shared libraries produced by a project `name` of version `M.m.p` are named `libname.so.M.m.p`. However, since when accessing shared libraries through the JVM, only the `name` can be specified and no particular version, the convention adopted by flow is to append `M` to the library name and always keep the major version at zero. E.g. `libflow.so.3.1.2` becomes `libflow3.so.0.1.2`.
