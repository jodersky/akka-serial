package com.github.jodersky.flow.samples.terminal

import akka.actor._

case object Read
case class ConsoleInput(in: String)
class ConsoleReader extends Actor {
  import context._
  
  def receive = {
    case Read => read()
  }
  
  def read() = {
    val in = Console.readLine()
    parent ! ConsoleInput(in)
  }

}