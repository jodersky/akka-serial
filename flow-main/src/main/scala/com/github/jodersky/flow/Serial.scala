package com.github.jodersky.flow

import akka.actor.ActorRef
import akka.actor.ExtensionKey
import akka.util.ByteString

/** Defines messages used by serial IO layer. */
object Serial extends ExtensionKey[SerialExt] {

  trait Command
  trait Event
  
  case class Open(handler: ActorRef, port: String, baud: Int, characterSize: Int = 8, twoStopBits: Boolean = false, parity: Parity.Parity = Parity.None) extends Command
  case class Opened(port: String) extends Event
  case class OpenFailed(port: String, reason: Throwable) extends Event
  
  case class Received(data: ByteString) extends Event
  
  case class Write(data: ByteString, ack: Boolean = false) extends Command
  case class Wrote(data: ByteString) extends Event
  
  case object Close extends Command
  case class Closed(error: Option[Exception]) extends Event
  
}
