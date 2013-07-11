package com.github.jodersky.flow

import java.io.IOException

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.github.jodersky.flow.internal.InternalSerial

import Serial.Open
import Serial.OpenFailed
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.SupervisorStrategy.Stop
import akka.actor.actorRef2Scala

class SerialManager extends Actor with ActorLogging {
  import SerialManager._
  import context._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: IOException => Stop
      case _: Exception => Escalate
    }

  def receive = {
    case Open(handler, port, baud, cs, tsb, parity) => Try { InternalSerial.open(port, baud, cs, tsb, parity.id) } match {
      case Failure(t) => {
        log.debug(s"failed to open low serial port at ${port}, baud ${baud}, reason: " + t.getMessage())
        handler ! OpenFailed(port, t)
      }

      case Success(serial) => {
        log.debug(s"opened low-level serial port at ${port}, baud ${baud}")
        context.actorOf(Props(classOf[SerialOperator], handler, serial), name = escapePortString(port))
      }

    }
  }

}

object SerialManager {

  private def escapePortString(port: String) = port collect {
    case '/' => '-'
    case c => c
  }

}