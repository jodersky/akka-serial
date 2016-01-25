package com.github.jodersky.flow

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated, actorRef2Scala }
import internal.{ Reader, SerialConnection, ThreadDied }
import java.nio.ByteBuffer

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
  client ! Serial.Opened(connection.port)
  reader.start()

  override def postStop = {
    connection.close()
  }

  def receive: Receive = {

    case Serial.Write(data, ack) => {
      writeBuffer.clear()
      data.copyToBuffer(writeBuffer)
      val sent = connection.write(writeBuffer)
      if (ack != Serial.NoAck) sender ! ack(sent)
    }

    case Serial.Close => {
      client ! Serial.Closed
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
