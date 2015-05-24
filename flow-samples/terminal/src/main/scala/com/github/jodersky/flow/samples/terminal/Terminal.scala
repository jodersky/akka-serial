package com.github.jodersky.flow
package samples.terminal

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated, actorRef2Scala }
import akka.io.IO
import akka.util.ByteString

class Terminal(port: String, settings: SerialSettings) extends Actor with ActorLogging {
  import Terminal._
  import context._

  val reader = actorOf(Props[ConsoleReader])

  log.info(s"Requesting manager to open port: ${port}, baud: ${settings.baud}")
  IO(Serial) ! Serial.Open(port, settings)

  override def postStop() = {
    system.shutdown()
  }

  def receive = {
    case Serial.CommandFailed(cmd, reason) => {
      log.error(s"Connection failed, stopping terminal. Reason: ${reason}")
      context stop self
    }
    case Serial.Opened(port) => {
      log.info(s"Port ${port} is now open.")
      val operator = sender
      context become opened(operator)
      context watch operator
      reader ! ConsoleReader.Read
    }
  }

  def opened(operator: ActorRef): Receive = {

    case Serial.Received(data) => {
      log.info(s"Received data: ${formatData(data)}")
    }

    case Terminal.Wrote(data) => log.info(s"Wrote data: ${formatData(data)}")

    case Serial.Closed => {
      log.info("Operator closed normally, exiting terminal.")
      context unwatch operator
      context stop self
    }

    case Terminated(`operator`) => {
      log.error("Operator crashed, exiting terminal.")
      context stop self
    }

    case ConsoleReader.EOT => {
      log.info("Initiating close.")
      operator ! Serial.Close
    }

    case ConsoleReader.ConsoleInput(input) => {
      val data = ByteString(input.getBytes)
      operator ! Serial.Write(data, length => Wrote(data.take(length)))
      reader ! ConsoleReader.Read
    }
  }

}

object Terminal {
  case class Wrote(data: ByteString) extends Serial.Event

  def apply(port: String, settings: SerialSettings) = Props(classOf[Terminal], port, settings)

  private def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + (new String(data.toArray, "UTF-8"))

}
