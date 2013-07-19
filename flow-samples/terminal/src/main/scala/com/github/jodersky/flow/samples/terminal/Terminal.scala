package com.github.jodersky.flow.samples.terminal

import com.github.jodersky.flow.Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString
import akka.io.IO
import com.github.jodersky.flow.Serial
import akka.actor.Terminated
import com.github.jodersky.flow.Parity
import akka.actor.Props

class Terminal(port: String, baud: Int, cs: Int, tsb: Boolean, parity: Parity.Parity) extends Actor with ActorLogging {
  import context._

  val reader = actorOf(Props[ConsoleReader])

  override def preStart() = {
    log.info(s"Requesting manager to open port: ${port}, baud: ${baud}")
    IO(Serial) ! Serial.Open(port, baud)
  }
  
  override def postStop() = {
    system.shutdown()
  }

  def receive = {
    case OpenFailed(reason, _, _, _, _, _) => {
      log.error(s"Connection failed, stopping terminal. Reason: ${reason}")
      context stop self
    }
    case Opened(port, _, _, _, _) => {
      log.info(s"Port ${port} is now open.")
      context become opened(sender)
      reader ! Read
    }
  }

  def opened(operator: ActorRef): Receive = {
    case Received(data) => {
      log.info(s"Received data: ${formatData(data)} (${new String(data.toArray, "UTF-8")})")
    }
    case Wrote(data) => log.info(s"Wrote data: ${formatData(data)} (${new String(data.toArray, "UTF-8")})")

    case Closed(x) => {
      x match {
        case None => log.info("Operator closed normally, exiting terminal.")
        case Some(ex) => log.error("Operator crashed, exiting terminal.")
      }
      context stop self
    }

    case ConsoleInput(":q") => {
      log.info("Initiating close.")
      operator ! Close
    }

    case ConsoleInput(input) => {
      operator ! Write(ByteString(input.getBytes), true)
      reader ! Read
    }
  }

  private def formatData(data: ByteString) = data.mkString("[", ",", "]")

}