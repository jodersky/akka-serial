package com.github.jodersky.flow.samples.broadcast

import com.github.jodersky.flow.SerialSettings
import com.github.jodersky.flow.internal.SerialConnection
import com.github.jodersky.flow.Parity
import java.nio.ByteBuffer
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

object Main {

  def ask(label: String, default: String) = {
    print(s"${label} [${default}]: ")
    val in = Console.readLine()
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

    println("Opening...")

    SerialConnection.debug(true)
    val c = SerialConnection.open(port, settings)

    val buffer = ByteBuffer.allocate(1024)

    val read = future {
      c.read(buffer)
    }.onFailure{
      case ex => println(ex)
    }

    println("Opened")
    Console.readLine()

    c.close()

    //val system = ActorSystem("flow")
    //val terminal = system.actorOf(Terminal(settings), name = "terminal")
    //system.registerOnTermination(println("Stopped terminal system."))
  }
}