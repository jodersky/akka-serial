package com.github.jodersky.flow

import com.github.jodersky.flow.internalial.Close;
import com.github.jodersky.flow.internalial.Closed;
import com.github.jodersky.flow.internalial.CommandFailed;
import com.github.jodersky.flow.internalial.Received;
import com.github.jodersky.flow.internalial.Write;
import com.github.jodersky.flow.internalial.Wrote;

import scala.concurrent.future
import scala.util.Failure
import scala.util.Success

import Serial.Close
import Serial.Closed
import Serial.CommandFailed
import Serial.Received
import Serial.Write
import Serial.Wrote
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.util.ByteString
import low.{Serial => LowSerial}

class SerialOperator(serial: LowSerial, handler: ActorRef) extends Actor with ActorLogging {
import context._

  object Reader extends Thread {
    private var continueReading = true

    override def run() {
      Thread.currentThread().setName("flow-reader " + serial.port)
      log.debug("started read thread " + Thread.currentThread().getName())
      while (continueReading) {
        try {
          log.debug("enter blocking read")
          val data = ByteString(serial.read())
          log.debug("return from blocking read")
          handler ! Received(data)
        } catch {
          case ex: PortInterruptedException => {
            continueReading = false
            log.debug("interrupted from blocking read")
          }
        }
      }
      log.debug("exit read thread normally " + Thread.currentThread().getName())
    }
  }

  Reader.start()

  context.watch(handler)

  def receive = {
    case c @ Write(data, ack) => {
      val writer = sender
      future { serial.write(data.toArray) }.onComplete {
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