package com.github.jodersky.flow
package samples.terminal

import com.github.jodersky.flow._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.ByteString

object Main {

  def defaultIfEmpty(in: String, default: String): String = if (in.isEmpty) default else in

  def ask(label: String, default: String) = {
    print(s"${label} [${default}]: ")
    val in = Console.readLine()
    println("")
    defaultIfEmpty(in, default)
  }

  def main(args: Array[String]): Unit = {
    val port = ask("Device", "/dev/ttyACM0")
    val baud = ask("Baud rate", "115200").toInt
    val cs = ask("Char size", "8").toInt
    val tsb = ask("Use two stop bits", "false").toBoolean
    val parity = Parity(ask("Parity [0=None, 1=Odd, 2=Even]", "0").toInt)
    println("Starting terminal, enter :q to exit.")
    
    internal.InternalSerial.debug(true)
    val system = ActorSystem("flow")
    val serial = system.actorOf(Props(classOf[SerialHandler], port, baud, cs, tsb, parity), name = "serial-handler")

    var continue = true
    while (continue) {
      val in = Console.readLine()
      if (in == ":q") {
        continue = false
        serial ! "close"
      } else {
        serial ! ByteString(in.getBytes())
      }
    }
    system.shutdown()
    println("Stopped terminal.")
  }
}