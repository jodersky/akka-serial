package com.github.jodersky.flow

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.github.jodersky.flow.internal.InternalSerial
import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import java.io.IOException

class SerialManager extends Actor with ActorLogging {
  import SerialManager._
  import context._

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: IOException => Stop
      case _: Exception => Escalate
    }

  def receive = {
    case Open(handler, port, baud) => Try { InternalSerial.open(port, baud) } match {
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