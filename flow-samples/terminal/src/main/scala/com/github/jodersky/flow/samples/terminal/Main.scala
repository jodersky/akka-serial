package com.github.jodersky.flow
package samples.terminal

import akka.actor.ActorSystem
import internal.SerialConnection
import scala.io.StdIn

object Main {

  def ask(label: String, default: String) = {
    print(label + " [" + default.toString + "]: ")
    val in = StdIn.readLine()
    println("")
    if (in.isEmpty) default else in
  }

  def main(args: Array[String]): Unit = {
    val port = ask("Device", "/dev/ttyACM0")
    val baud = ask("Baud rate", "115200").toInt
    val cs = ask("Char size", "8").toInt
    val tsb = ask("Use two stop bits", "false").toBoolean
    val parity = Parity(ask("Parity (0=None, 1=Odd, 2=Even)", "0").toInt)
    val settings = SerialSettings(baud, cs, tsb, parity)

    println("Starting terminal system, enter :q to exit.")
    SerialConnection.debug(true)
    val system = ActorSystem("flow")
    val terminal = system.actorOf(Terminal(port, settings), name = "terminal")
    system.registerOnTermination(println("Stopped terminal system."))
  }
}
