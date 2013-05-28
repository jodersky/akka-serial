package com.github.jodersky.flow

import scala.util.Failure
import scala.util.Success

import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString
import low.{ Serial => LowSerial }

class SerialOperator(serial: LowSerial, handler: ActorRef) extends Actor {
  import context._

  context.watch(handler)
  startRead()

  def receive = {
    case c @ Write(data) => {
      val writer = sender
      serial.write(data.toArray).onComplete {
        case Success(data) => writer ! Wrote(ByteString(data))
        case Failure(t) => writer ! CommandFailed(c, t)
      }
    }
    case Close => {
      context.stop(self)
    }
  }

  private def startRead(): Unit = {
    val futureData = serial.read()
    futureData.onComplete {
      case Failure(t) => {
        handler ! Closed(t)
        context.stop(self)
      }
      case Success(data) => {
        handler ! Received(ByteString(data))
        startRead()
      }
    }
  }

  override def postStop = {
    serial.close()
  }

}