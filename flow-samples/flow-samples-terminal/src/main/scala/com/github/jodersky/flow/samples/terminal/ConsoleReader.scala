package com.github.jodersky.flow.samples.terminal

import akka.actor.Actor
import akka.actor.actorRef2Scala

class ConsoleReader extends Actor {
  import context._
  import ConsoleReader._

  def receive = {
    case Read =>
      Console.readLine() match {
        case ":q" => parent ! EOT
        case s => parent ! ConsoleInput(s)
      }
  }

}

object ConsoleReader {

  case object Read
  case object EOT
  case class ConsoleInput(in: String)

}