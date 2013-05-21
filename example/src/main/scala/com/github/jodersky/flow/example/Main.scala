package com.github.jodersky.flow.example

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.jodersky.flow.Serial
import scala.util.Try
import scala.util.Success

object Main {

  def main(args: Array[String]): Unit = {
    val isInt = Try(args(1).toInt) match {case Success(_) => true; case _ => false}
    if (!(args.length == 2 && isInt)) {
      println("invalid parameters")
      println("parameters: port baud")
      println("example: /dev/ttyACM0 115200")
      return
    }
    val port = args(0)
    val baud = args(1).toInt

    Serial.debug(true)
    
    println("opening " + port)
    val serial = Serial.open(port, baud) { data =>
      println("received: " + data.mkString("[", ",", "]"))
    }
    
    println("press enter to write a looong array of data")
    Console.readLine()

    val data: Array[Byte] = Array.fill(100)(42)
    serial.write(data).map(d => println("wrote: " + d.mkString("[", ",", "]"))).recover { case t => println("write error") }

    println("press enter to exit")
    Console.readLine()
    
    println("exiting")
    Console.flush()
  }
}