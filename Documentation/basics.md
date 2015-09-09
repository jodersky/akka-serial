# Communication Protocol
The following is a general guide on the usage of flow. If you prefer a complete example, check out the code contained in the `flow-samples` directory. 

Flow's API follows that of an actor based system, where each actor is assigned specific functions involved in serial communication. The two main actor types are:

1. Serial "manager". The manager is a singleton actor that is instantiated once per actor system, a reference to it may be obtained with `IO(Serial)`. It is typically used to open serial ports (see following section).

2. Serial "operators". Operators are created once per open serial port and serve as an intermediate between client code and native code dealing with serial data transmission and reception. They isolate the user from threading issues and enable the reactive dispatch of incoming data. A serial operator is said to be "associated" to its underlying open serial port.

The messages understood by flow's actors are all contained in the `com.github.jodersky.flow.Serial` object. They are well documented and should serve as the entry point when searching the API documentation.

## Opening a Port
A serial port is opened by sending an `Open` message to the serial manager. The response varies on the outcome of opening the underlying serial port.

1. In case of failure, the serial manager will respond with a `CommandFailed` message to the original sender. The message contains details on the reason to why the opening failed.

2. In case of success, the sender is notified with an `Opened` message. This message is sent from an operator actor, spawned by the serial manager. It is useful to capture the sender (i.e. the operator) of this message as all further communication with the newly opened port must pass through the operator.

```scala
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
```

## Writing Data
Writing data is as simple as sending a `Write` message to an operator. The data to send is an instance of `akka.util.ByteString`:

```scala
operator ! Serial.Write(data)
```

Optionally, an acknowledgement for sent data can be requested by adding an `ack` parameter to a `Write` message. The `ack` parameter is of type `Int => Serial.Event`, i.e. a function that takes the number of actual bytes written and returns an event. Note that "bytes written" refers to bytes enqueued in a kernel buffer; no guarantees can be made on the actual transmission of the data.

```scala

case class MyPacketAck(wrote: Int) extends Serial.Event

operator ! Serial.Write(data, MyPacketAck(_))
operator ! Serial.Write(data, n => MyPacketAck(n))

def receive = {
  case MyPacketAck(n) => println("Wrote " + n + " bytes of data")
}

```

## Receiving Data
The actor that opened a serial port (referred to as the client), exclusively receives incomming messages from the operator. These messages are in the form of `akka.util.ByteString`s and wrapped in a `Received` object.

```scala
def receive = {
  case Serial.Received(data) => println("Received data: " + data.toString)
}
```

## Closing a Port
A port is closed by sending a `Close` message to its operator:
```scala
operator ! Serial.Close
```
The operator will close the underlying serial port and respond with a final `Closed` message before terminating.


## Resources and Error Handling
The operator has a deathwatch on the client actor that opened the port, this means that if the latter crashes, the operator closes the port and equally terminates, freeing any allocated resources.

The opposite is not true by default, i.e. if the operator crashes (this can happen for example on IO errors) it dies silently and the client is not informed. Therefore, it is recommended that the client keep a deathwatch on the operator.
