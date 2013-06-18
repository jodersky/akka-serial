package com.github.jodersky.flow

import akka.io._
import akka.actor.ExtensionKey
import akka.actor.ExtendedActorSystem
import akka.actor.Props
import low.{ Serial => LowSerial }
import akka.actor.ActorRef
import akka.util.ByteString

object Serial extends ExtensionKey[SerialExt] {

  trait Command
  trait Event
  
  case class Open(handler: ActorRef, port: String, baud: Int) extends Command
  case class Opened(operator: ActorRef) extends Event
  
  case class Received(data: ByteString) extends Event
  
  case class Write(data: ByteString) extends Command
  case class Wrote(data: ByteString) extends Event
  
  case object Close extends Command
  case object Closed extends Event

  case class CommandFailed(command: Command, reason: Throwable) extends Event
  
  
}

class SerialExt(system: ExtendedActorSystem) extends IO.Extension {
  def manager = system.actorOf(Props[SerialManager], name = "IO-SERIAL")
}