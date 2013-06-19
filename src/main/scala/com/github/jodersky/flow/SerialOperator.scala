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
  
  object Reader extends Thread {
    private var continueReading = true 
    
    override def run() {
      Thread.currentThread().setName("flow-reader" + serial.port)
      while (continueReading) {
        println("beginning read")
        val data = ByteString(serial.read())
        println("return from read")
        handler ! Received(data)
      }
    }
  }
  
  Reader.start()
  
  context.watch(handler)

  def receive = {
    case c @ Write(data) => {
      val writer = sender
      future{serial.write(data.toArray)}.onComplete {
        case Success(data) => writer ! Wrote(ByteString(data))
        case Failure(t) => writer ! CommandFailed(c, t)
      }
    }
    
    case Close => {
      sender ! Closed
      context.stop(self)
    }
  }

  override def postStop = {
    serial.close()
  }

}