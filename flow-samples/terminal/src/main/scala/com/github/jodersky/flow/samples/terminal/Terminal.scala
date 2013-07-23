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
  import Terminal._
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
    case CommandFailed(cmd, reason) => {
      log.error(s"Connection failed, stopping terminal. Reason: ${reason}")
      context stop self
    }
    case Opened(port, _, _, _, _) => {
      log.info(s"Port ${port} is now open.")
      val operator = sender
      context become opened(operator)
      context watch operator 
      operator ! Register(self)
      reader ! Read
    }
  }

  def opened(operator: ActorRef): Receive = {
    
    case Received(data) => {
      log.info(s"Received data: ${formatData(data)} (${new String(data.toArray, "UTF-8")})")
    }
    
    case Wrote(data) => log.info(s"Wrote data: ${formatData(data)} (${new String(data.toArray, "UTF-8")})")

    case Closed => {
      log.info("Operator closed normally, exiting terminal.")
      context unwatch operator
      context stop self
    }
    
    case Terminated(`operator`) => {
      log.error("Operator crashed, exiting terminal.")
      context stop self
    }

    case ConsoleInput(":q") => {
      log.info("Initiating close.")
      operator ! Close
    }

    case ConsoleInput(input) => {
      val data = ByteString(input.getBytes)
      operator ! Write(data, Wrote(data))
      reader ! Read
    }
  }

  private def formatData(data: ByteString) = data.mkString("[", ",", "]")

}

object Terminal {
  case class Wrote(data: ByteString) extends Event
}