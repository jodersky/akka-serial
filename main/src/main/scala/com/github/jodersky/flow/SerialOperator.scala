package com.github.jodersky.flow

import scala.util.Failure
import scala.util.Success
import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString
import low.{ Serial => LowSerial }
import scala.util.Try
import scala.concurrent._

class SerialOperator(serial: LowSerial, handler: ActorRef) extends Actor {
  import context._

  context.watch(handler)
  
  class Reader extends Actor {
    while (true) {
      val data = ByteString(serial.read())
      handler ! Received(data)
    }
  }

  def receive = {
    case Write(data) => {
      val writer = sender
      future{serial.write(data.toArray)}.onComplete {
        case Success(data) => writer ! Wrote(ByteString(data))
        case Failure(t) => writer ! CommandFailed(c, t)
      }
    }
    
    case Close => {
      context.stop(self)
    }
  }

  override def postStop = {
    serial.close()
  }

}