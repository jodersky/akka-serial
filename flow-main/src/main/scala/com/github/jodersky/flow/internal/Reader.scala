package com.github.jodersky.flow.internal

import java.nio.ByteBuffer

import com.github.jodersky.flow.PortInterruptedException
import com.github.jodersky.flow.Serial.Received

import akka.actor.Actor
import akka.actor.ActorRef
import akka.util.ByteString

class Reader(serial: SerialConnection, buffer: ByteBuffer, operator: ActorRef, client: ActorRef) extends Thread {
  def readLoop() = {
    var stop = false
    while (!serial.isClosed && !stop) {
      try {
        buffer.clear()
        val length = serial.read(buffer)
        buffer.limit(length)
        val data = ByteString.fromByteBuffer(buffer)
        client.tell(Received(data), operator)
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
