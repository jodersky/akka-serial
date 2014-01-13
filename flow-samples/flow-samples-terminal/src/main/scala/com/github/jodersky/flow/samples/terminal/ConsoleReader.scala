package com.github.jodersky.flow.samples.terminal

import akka.actor._
import java.io.BufferedReader
import java.io.InputStreamReader

class ConsoleReader extends Actor {
  import context._
  import ConsoleReader._

  def receive = {
    case Read => read() match {
      case Some(input) => parent ! ConsoleInput(input)
      case None => parent ! EOT
    }
  }

  def read(): Option[String] = {
    val eot = 4
    val line = Console.readLine
    if (line == ":q") None else Some(line)
  }

}

object ConsoleReader {

  case object Read

  case object EOT
  case class ConsoleInput(in: String)

}