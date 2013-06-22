package com.github.jodersky.flow
package example

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.util.Try

import com.github.jodersky.flow.Serial
import com.github.jodersky.flow.Serial._

import akka.actor.ActorSystem
import akka.actor.Props
import akka.io.IO
import akka.util.ByteString

object Main {

  def main(args: Array[String]): Unit = {
    /*val isInt = Try(args(1).toInt) match { case Success(_) => true; case _ => false }
    if (!(args.length == 2 && isInt)) {
      println("invalid parameters")
      println("parameters: port baud")
      println("example: /dev/ttyACM0 115200")
      return
    }*/
    val port = "/dev/ttyACM0"
    val baud = 115200

    low.Serial.debug(true)
    
    implicit val system = ActorSystem("flow")
    val serial = system.actorOf(Props(classOf[SerialHandler], port, baud), name = "serial-handler")
        
    readLine()
    serial ! ByteString(42)
    
    readLine()
    serial ! "close"
    readLine()
    
    system.shutdown()
  }
}