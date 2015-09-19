[![Download](https://api.bintray.com/packages/jodersky/maven/flow/images/download.svg)](https://bintray.com/jodersky/maven/flow/_latestVersion)
[![Join the chat at https://gitter.im/jodersky/flow](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jodersky/flow?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications.

## Highlights
- Reactive: only does work when required (no constant polling of ports or blocking IO)
- Integrates seamlessly with Akka
- Portable to POSIX systems
- Watchable ports: react to connection of new devices

## Usage
See [Documentation](Documentation/README.md) for a guide on flow.

For the impatient, here is a quick example.

```scala
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.io.IO
import akka.util.ByteString
import com.github.jodersky.flow.{Serial, SerialSettings}

/**
 *  Sample actor representing a simple terminal.
 */
class Terminal(port: String, settings: SerialSettings) extends Actor with ActorLogging {
  import context._

  override def preStart() = {
    log.info(s"Requesting manager to open port: ${port}, baud: ${settings.baud}")
    IO(Serial) ! Serial.Open(port, settings)
  }

  def receive: Receive = {

    case Serial.CommandFailed(cmd, reason) =>
      log.error(s"Connection failed, stopping terminal. Reason: ${reason}")
      context stop self

    case Serial.Opened(port) =>
      log.info(s"Port ${port} is now open.")
      context become opened(sender)
      context watch sender // get notified in the event the operator crashes

  }

  def opened(operator: ActorRef): Receive = {

    case Serial.Received(data) =>
      log.info(s"Received data: " + data)

    case Serial.Closed =>
      log.info("Operator closed normally, exiting terminal.")
      context stop self

    case Terminated(`operator`) =>
      log.error("Operator crashed unexpectedly, exiting terminal.")
      context stop self

    case ":q" =>
      operator ! Serial.Close

    case str: String =>
      operator ! Serial.Write(ByteString(str))

  }

}

object Terminal {
  def apply(port: String, settings: SerialSettings) =
    Props(classOf[Terminal], port, settings)
}
```

More examples on flow's usage are located in the `flow-samples` directory. The examples may be run with sbt: `flow-samples-<sample_name>/run`.

Since flow integrates into the Akka-IO framework, a good resource on its general design is the framework's [documentation](http://doc.akka.io/docs/akka/2.4.0/scala/io.html).

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Directory Structure
```
flow/
├── Documentation         Documentation files.
├── flow-main             Main scala source files.
├── flow-native           C sources used to back serial communication.
├── flow-samples          Runnable example projects.
└── project               SBT configuration.
```

## Build
See detailed documentation in [Documentation/building.md](Documentation/building.md) on how to build and install flow.

## Copying
flow is released under the terms of the 3-clause BSD license. See LICENSE for details.
