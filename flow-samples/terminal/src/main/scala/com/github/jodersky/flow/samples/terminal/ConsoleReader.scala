package com.github.jodersky.flow
package samples.terminal

import akka.actor.Actor
import scala.io.StdIn

class ConsoleReader extends Actor {
  import context._
  import ConsoleReader._

  def receive = {
    case Read =>
      StdIn.readLine() match {
        case ":q" | null => parent ! EOT
        case s => {
          parent ! ConsoleInput(s)
        }
      }
  }

}

object ConsoleReader {

  case object Read
  case object EOT
  case class ConsoleInput(in: String)

}
