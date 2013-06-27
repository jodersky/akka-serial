package com.github.jodersky.flow
package example

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
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

    // InternalSerial.debug(true)

    implicit val system = ActorSystem("flow")
    val serial = system.actorOf(Props(classOf[SerialHandler], port, baud), name = "serial-handler")

    readLine()
    serial ! ByteString("hello back".getBytes())

    readLine()
    serial ! "close"
    readLine()

    system.shutdown()
  }
}