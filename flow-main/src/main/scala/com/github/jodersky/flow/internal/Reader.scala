package com.github.jodersky.flow
package internal

import akka.actor.{ Actor, ActorRef }
import akka.util.ByteString
import java.nio.ByteBuffer

class Reader(serial: SerialConnection, buffer: ByteBuffer, operator: ActorRef, client: ActorRef) extends Thread {
  def readLoop() = {
    var stop = false
    while (!serial.isClosed && !stop) {
      try {
        buffer.clear()
        val length = serial.read(buffer)
        buffer.limit(length)
        val data = ByteString.fromByteBuffer(buffer)
        client.tell(Serial.Received(data), operator)
      } catch {

        //don't do anything if port is interrupted
        case ex: PortInterruptedException => {}

        //stop and tell operator on other exception
        case ex: Exception => {
          stop = true
          operator.tell(ThreadDied(this, ex), Actor.noSender)
        }
      }
    }
  }

  override def run() {
    this.setName("flow-reader " + serial.port)
    readLoop()
  }
}
