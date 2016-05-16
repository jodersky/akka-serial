package ch.jodersky.flow

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }
import akka.util.ByteString
import java.nio.ByteBuffer

/**
  * Operator associated to an open serial port. All communication with a port is done via an operator. Operators are created though the serial manager.
  * @see SerialManager
  */
private[flow] class SerialOperator(connection: SerialConnection, bufferSize: Int, client: ActorRef) extends Actor {
  import SerialOperator._
  import context._

  case class ReaderDied(ex: Throwable)
  object Reader extends Thread {
    val buffer = ByteBuffer.allocateDirect(bufferSize)

    def loop() = {
      var stop = false
      while (!connection.isClosed && !stop) {
        try {
          buffer.clear()
          val length = connection.read(buffer)
          buffer.limit(length)
          val data = ByteString.fromByteBuffer(buffer)
          client.tell(Serial.Received(data), self)
        } catch {
          // don't do anything if port is interrupted
          case ex: PortInterruptedException => {}

          //stop and tell operator on other exception
          case ex: Exception =>
            stop = true
            self.tell(ReaderDied(ex), Actor.noSender)
        }
      }
    }

    override def run() {
      this.setName(s"serial-reader(${connection.port})")
      loop()
    }

  }

  val writeBuffer = ByteBuffer.allocateDirect(bufferSize)

  override def preStart() = {
    context watch client
    client ! Serial.Opened(connection.port)
    Reader.start()
  }

  override def receive: Receive = {

    case Serial.Write(data, ack) =>
      writeBuffer.clear()
      data.copyToBuffer(writeBuffer)
      val sent = connection.write(writeBuffer)
      if (ack != Serial.NoAck) sender ! ack(sent)

    case Serial.Close =>
      client ! Serial.Closed
      context stop self

    case Terminated(`client`) =>
      context stop self

    // go down with reader thread
    case ReaderDied(ex) => throw ex

  }

  override def postStop() = {
    connection.close()
  }

}

private[flow] object SerialOperator {
  def apply(connection: SerialConnection, bufferSize: Int, client: ActorRef) = Props(classOf[SerialOperator], connection, bufferSize, client)
}
