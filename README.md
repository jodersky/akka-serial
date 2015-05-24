[ ![Download](https://api.bintray.com/packages/jodersky/maven/flow/images/download.svg) ](https://bintray.com/jodersky/maven/flow/_latestVersion)

# flow

[![Join the chat at https://gitter.im/jodersky/flow](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jodersky/flow?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications.

## Motivation
The main reason for yet another serial communication library for the JVM is that all other libraries tested used blocking IO and/or consumed enormous amounts of CPU while being idle. Flow's main goal is therefore to provide a lightweight library that only does work when communication is required. This reactive concept integrates well with the Akka IO layer therefore making flow an ideal library for extending it.

## Getting Started
Flow is built and its examples run with SBT. To get started, include a dependency to flow in your project:

    libraryDependencies += "com.github.jodersky" %% "flow" % "2.1.2"

*ATTENTION*: flow uses native libraries to back serial communication, therefore before you can run any application depending on flow you must include flow's native library! To do so, you have two options.

1.  The easy way: add a second dependency to your project:
        
        libraryDependencies += "com.github.jodersky" % "flow-native" % "2.1.2"

    This will add a jar to your classpath containing native libraries for various platforms. At run time, the correct library for the current platform is selected, extracted and loaded. This solution enables running applications seamlessly, as if they were pure JVM applications. However, since the JVM does not enable full determination of the current platform (only OS and rough architecture are known), only a couple of platforms can be supported through this solution at the same time. Currently, these are given in the table below.

    | OS                | Architecture         | Notes                                                                  |
    |-------------------|----------------------|------------------------------------------------------------------------|
    | Linux | x86<br>x86_64<br>ARM (v7, hardfloat ABI) | A user accessing a serial port may need to be in the dialout group. |
    | Mac OS X | x86_64               | Use /dev/cu* device instead of /dev/tty*.                              |


2.  Maximum scalability: do not include the second dependency above. Instead, for every end-user application that relies on flow, manually add the native library for the current platform to the JVM's library path. This can be achieved through various ways, notably:
      - Per application:
        Run your program with the command-line option ```-Djava.library.path=".:<folder containing libflow.so>"```. E.g. ```java -Djava.library.path=".:/home/<folder containing libflow.so>" -jar your-app.jar```

      - System- or user-wide:
          - Copy the native library to a place that is on the default java library path and run your application normally. Such places usually include /usr/lib and /usr/local/lib.
          - Use a provided installer (currently debian archive and mac .pkg, available in releases)

    The native library can either be obtained by building flow (see section Build) or by taking a pre-compiled one, found in releases in the github project. Native libraries need to be of major version 3 to work with this version of flow.

It is recomended that you use the first option only for testing purposes or end-user applications. The second option is recomended for libraries, since it leaves more choice to the end-user.

## Basic usage
See [Documentation/basics.md](Documentation/basics.md) for a short guide on using flow. For the impatient, here is a quick example.

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
      log.info(s"Receivd data: " + data)

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
  def apply(port: String, settings: SerialSettings) = Props(classOf[Terminal], port, settings)
}
```

## Examples
Examples on flow's usage are located in the flow-samples directory. The examples may be run by switching to the corresponding project in sbt: `project samples-<sample_name>` and typing `run`. Be sure to connect a serial device before running an example.

Since flow integrates into the Akka-IO framework, a good resource on its general design is the framework's [documentation](http://doc.akka.io/docs/akka/2.3.10/scala/io.html).

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Build
See detailed documentation in [Documentation/building.md](Documentation/building.md) on how to build and install flow.

## Copying
flow is released under the terms of the 3-clause BSD license. See LICENSE for details.
