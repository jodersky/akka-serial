package ch.jodersky.flow

import akka.actor.{ Actor, ActorLogging, OneForOneStrategy }
import akka.actor.SupervisorStrategy.{ Escalate, Stop }
import scala.util.{ Failure, Success, Try }

/**
 * Entry point to the serial API. Actor that manages serial port creation. Once opened, a serial port is handed over to
 * a dedicated operator actor that acts as an intermediate between client code and the native system serial port.
 * @see SerialOperator
 */
private[flow] class SerialManager extends Actor {
  import SerialManager._
  import context._

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception if sender == watcher => Escalate
    case _: Exception => Stop
  }

  private val watcher = actorOf(Watcher(self), "watcher")

  def receive = {

    case open @ Serial.Open(port, settings, bufferSize) => Try {
      SerialConnection.open(port, settings)
    } match {
      case Success(connection) => context.actorOf(SerialOperator(connection, bufferSize, sender), name = escapePortString(connection.port))
      case Failure(err) => sender ! Serial.CommandFailed(open, err)
    }

    case w: Serial.Watch => watcher.forward(w)

    case u: Serial.Unwatch => watcher.forward(u)

  }

}

private[flow] object SerialManager {

  private def escapePortString(port: String) = port map {
    case '/' => '-'
    case c => c
  }

}
