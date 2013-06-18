package com.github.jodersky.flow

import akka.actor.Actor
import Serial._
import low.{ Serial => LowSerial }
import scala.util.Success
import scala.util.Failure
import akka.actor.Props
import scala.concurrent._

class SerialManager extends Actor {
  import SerialManager._
  import context._

  def receive = {
    case command @ Open(handler, port, baud) =>
      future{LowSerial.open(port, baud)}.onComplete(_ match {
        case Success(serial) => {
          val operator = context.actorOf(Props(classOf[SerialOperator], serial, handler), name = escapePortString(port))
          handler ! Opened(operator)
        }
        case Failure(t) => sender ! CommandFailed(command, t)
      })
  }

}

object SerialManager {
  
  private def escapePortString(port: String) = port collect {
    case '/' => '-'
    case c => c
  }
  
}