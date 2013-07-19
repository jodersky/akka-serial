package com.github.jodersky.flow

import java.io.IOException

import com.github.jodersky.flow.internal.InternalSerial

import Serial.Close
import Serial.Closed
import Serial.Opened
import Serial.Received
import Serial.Write
import Serial.Wrote
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.util.ByteString

/** Operator associated to an open serial port. All communication with a port is done via an operator. Operators are created though the serial manager. */
class SerialOperator(handler: ActorRef, serial: InternalSerial) extends Actor with ActorLogging {
  import SerialOperator._
  import context._

  private object Reader extends Thread {

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
      enterReadLoop()
    }

  }

  override def preStart() = {
    context watch handler
    handler ! Opened(serial.port, serial.baud, serial.characterSize, serial.twoStopBits, Parity(serial.parity))
    Reader.start()
  }

  override def postStop = {
    serial.close()
  }

  def receive: Receive = {

    case Write(data, ack) => {
      try {
        val sent = serial.write(data.toArray)
        if (ack) sender ! Wrote(ByteString(sent))
      } catch {
        case ex: IOException => {
          handler ! Closed(Some(ex))
          context stop self
        }
      }
    }

    case Close => {
      handler ! Closed(None)
      context stop self
    }

    case Terminated(`handler`) => context.stop(self)

    //go down with reader thread
    case ReadException(ex) => {
      handler ! Closed(Some(ex))
      context stop self
    }

  }

}

object SerialOperator {
  private case class ReadException(ex: Exception)
}