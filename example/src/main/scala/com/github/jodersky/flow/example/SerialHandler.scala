package com.github.jodersky.flow.example

import com.github.jodersky.flow.Serial._
import com.github.jodersky.flow.low.{Serial => LowSerial}
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.util.ByteString

class SerialHandler extends Actor with ActorLogging {
  var operator: Option[ActorRef] = None
  
  def receive = {
    case Opened(operator) => this.operator = Some(operator)
    
    case CommandFailed(cmd, reason) =>
      println(s"command ${cmd} failed, reason: ${reason}")
      
    case Received(data) => println("received data: " + formatData(data))
      
    case Close =>
      operator.map(_ ! Close)
      
    case Closed(_) => println("port closed")
      
    case Write(data) => {
      operator.map(_ ! Write(data))
    }
    
    case Wrote(data) => println("wrote data: " + formatData(data))
    
  }
  
  private def formatData(data: ByteString) = data.mkString("[",",","]")

}