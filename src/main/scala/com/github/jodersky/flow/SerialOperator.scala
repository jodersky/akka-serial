package com.github.jodersky.flow

import scala.concurrent.future
import scala.util.Failure
import scala.util.Success
import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString
import com.github.jodersky.flow.internal.InternalSerial
import akka.actor.Terminated
import scala.util.Try

class SerialOperator(handler: ActorRef, serial: InternalSerial) extends Actor with ActorLogging {
  import context._

  case class ReadException(ex: Throwable)

  object Reader extends Thread {

    def enterReadLoop() = {
      var continueReading = true
      while (continueReading) {
        try {
          val data = ByteString(serial.read())
          handler ! Received(data)
        } catch {

          //port is closing, stop thread gracefully
          case ex: PortInterruptedException => {
            continueReading = false
          }

          //something else went wrong stop and tell actor
          case ex: Exception => {
            continueReading = false
            self ! ReadException(ex)
          }
        }
      }
    }

    def name = this.getName()
    
    override def run() {
      this.setName("flow-reader " + serial.port)
      log.debug(name + ": started reader thread")
      enterReadLoop()
      log.debug(name + ": exiting")
    }
    
  }

  override def preStart() = {
    context watch handler
    handler ! Opened(serial.port)
    Reader.start()
  }

  override def postStop = {
    serial.close()
  }

  def receive: Receive = {

    case Write(data, ack) => {
      serial.write(data.toArray) // no future needed as write is non-blocking
      if (ack) sender ! Wrote(data)
    }

    case Close => {
      sender ! Closed
      context.stop(self)
    }

    case Terminated(`handler`) => context.stop(self)

    //go down with reader thread
    case ReadException(ex) => throw ex

  }

}