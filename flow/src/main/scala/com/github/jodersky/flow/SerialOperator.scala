package com.github.jodersky.flow

import java.io.IOException
import com.github.jodersky.flow.internal.InternalSerial
import Serial._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.util.ByteString
import scala.collection.mutable.HashSet
import akka.actor.Props

/**
 * Operator associated to an open serial port. All communication with a port is done via an operator. Operators are created though the serial manager.
 *  @see SerialManager
 */
class SerialOperator(serial: InternalSerial, client: ActorRef) extends Actor with ActorLogging {
  import SerialOperator._
  import context._

  private object Reader extends Thread {
    def readLoop() = {
      var continueReading = true
      while (continueReading) {
        try {
          val data = ByteString(serial.read())
          client ! Received(data)
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

    override def run() {
      this.setName("flow-reader " + serial.port)
      readLoop()
    }
  }
  
  
  client ! Opened(serial.port)
  context.watch(client)
  Reader.start()
  

  override def postStop = {
    serial.close()
  }

  def receive: Receive = {

    case Write(data, ack) => {
      val sent = serial.write(data.toArray)
      if (ack != NoAck) sender ! ack
    }

    case Close => {
      client ! Closed
      context stop self
    }

    //go down with reader thread
    case ReadException(ex) => throw ex

  }

}

object SerialOperator {
  private case class ReadException(ex: Exception)
  
  def apply(serial: InternalSerial, client: ActorRef) = Props(classOf[SerialOperator], serial, client)
}