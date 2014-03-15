package com.github.jodersky.flow

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.github.jodersky.flow.internal.InternalSerial

import Serial.CommandFailed
import Serial.Open
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala

/**
 * Actor that manages serial port creation. Once opened, a serial port is handed over to
 * a dedicated operator actor that acts as an intermediate between client code and the native system serial port.
 * @see SerialOperator
 */
class SerialManager extends Actor with ActorLogging {
  import SerialManager._
  import context._

  def receive = {
    case open @ Open(port, baud, characterSize, twoStopBits, parity) => Try {
      InternalSerial.open(port, baud, characterSize, twoStopBits, parity.id)
    } match {
      case Success(internal) => context.actorOf(SerialOperator(internal, sender), name = escapePortString(internal.port))
      case Failure(err) => sender ! CommandFailed(open, err)
    }
  }

}

object SerialManager {

  private def escapePortString(port: String) = port collect {
    case '/' => '-'
    case c => c
  }

}