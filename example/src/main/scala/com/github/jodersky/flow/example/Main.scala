package com.github.jodersky.flow.example

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.jodersky.flow.Serial

object Main {

  def main(args: Array[String]): Unit = {
    println("opening")
    
    Serial.debug(true)
    val s = Serial.open("/dev/ttyACM0", 115200){ data =>
      println(data.mkString("[",",","]"))
    }
    println("press enter to write a loooooooooong array of data")
    Console.readLine()
    
    val data: Array[Byte] = Array.fill(1000000)(42)
    s.write(data).map(d => println("sent: " + d.mkString("[",",","]"))).recover{case t => println("write error")}
    
    println("press enter to exit")
    Console.readLine()
    println("exiting")
    Console.flush()
  }
}