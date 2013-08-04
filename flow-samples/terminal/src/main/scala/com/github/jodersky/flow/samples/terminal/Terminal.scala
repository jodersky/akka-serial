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
import com.github.jodersky.flow.SerialSettings

class Terminal(settings: SerialSettings) extends Actor with ActorLogging {
  import Terminal._
  import context._

  val reader = actorOf(Props[ConsoleReader])

  override def preStart() = {
    log.info(s"Requesting manager to open port: ${settings.port}, baud: ${settings.baud}")
    IO(Serial) ! Serial.Open(settings)
  }
  
  override def postStop() = {
    system.shutdown()
  }

  def receive = {
    case CommandFailed(cmd, reason) => {
      log.error(s"Connection failed, stopping terminal. Reason: ${reason}")
      context stop self
    }
    case Opened(s, _) => {
      log.info(s"Port ${s.port} is now open.")
      val operator = sender
      context become opened(operator)
      context watch operator 
      operator ! Register(self)
      reader ! ConsoleReader.Read
    }
  }

  def opened(operator: ActorRef): Receive = {
    
    case Received(data) => {
      log.info(s"Received data: ${formatData(data)}")
    }
    
    case Wrote(data) => log.info(s"Wrote data: ${formatData(data)}")

    case Closed => {
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
      operator ! Close
    }

    case ConsoleReader.ConsoleInput(input) => {
      val data = ByteString(input.getBytes)
      operator ! Write(data, Wrote(data))
      reader ! ConsoleReader.Read
    }
  }

  

}

object Terminal {
  case class Wrote(data: ByteString) extends Event
  
  def apply(settings: SerialSettings) = Props(classOf[Terminal], settings)
  
  private def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + (new String(data.toArray, "UTF-8"))
  
}