package com.github.jodersky.flow

import java.nio.ByteBuffer

import com.github.jodersky.flow.internal.Reader
import com.github.jodersky.flow.internal.ThreadDied
import com.github.jodersky.flow.internal.SerialConnection

import Serial.Close
import Serial.Closed
import Serial.NoAck
import Serial.Opened
import Serial.Write
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala

/**
 * Operator associated to an open serial port. All communication with a port is done via an operator. Operators are created though the serial manager.
 * @see SerialManager
 */
class SerialOperator(connection: SerialConnection, bufferSize: Int, client: ActorRef) extends Actor with ActorLogging {
  import SerialOperator._
  import context._

  val readBuffer = ByteBuffer.allocateDirect(bufferSize)
  val reader = new Reader(connection, readBuffer, self, client)
  val writeBuffer = ByteBuffer.allocateDirect(bufferSize)

  context.watch(client)
  client ! Opened(connection.port)
  reader.start()

  override def postStop = {
    connection.close()
  }

  def receive: Receive = {

    case Write(data, ack) => {
      writeBuffer.clear()
      data.copyToBuffer(writeBuffer)
      val sent = connection.write(writeBuffer)
      if (ack != NoAck) sender ! ack(sent)
    }

    case Close => {
      client ! Closed
      context stop self
    }
    
    case Terminated(`client`) => {
      context stop self
    }

    //go down with reader thread
    case ThreadDied(`reader`, ex) => throw ex

  }

}

object SerialOperator {
  def apply(connection: SerialConnection, bufferSize: Int, client: ActorRef) = Props(classOf[SerialOperator], connection, bufferSize, client)
}
