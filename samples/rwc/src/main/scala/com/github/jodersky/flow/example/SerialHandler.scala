package com.github.jodersky.flow.example

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.low.{ Serial => LowSerial }
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString
import akka.io.IO
import com.github.jodersky.flow.Serial


class SerialHandler(port: String, baud: Int) extends Actor with ActorLogging {
  import context._
  
  println(s"Requesting port open: ${port}, baud: ${baud}")
  IO(Serial) ! Serial.Open(self, port, baud)
  

  def receive = {
    case CommandFailed(_: Open, reason) => {
      println(s"connection failed, reason: ${reason}")
      context stop self
    }

    case Opened(operator) =>
      println("Port opened.")
      context become {
        case Received(data) => {
          println("received data: " + formatData(data))
          println("as string: " + new String(data.toArray, "UTF-8"))
        }
        case Wrote(data) => println("wrote ACK: " + formatData(data))
        case CommandFailed(_, _) => println("write failed")
        case Closed => context stop self
        case "close" => operator ! Close
        case data: ByteString => operator ! Write(data) 
      }
  }
  
  private def formatData(data: ByteString) = data.mkString("[", ",", "]")

}