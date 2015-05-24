package com.github.jodersky.flow
package samples.watcher

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.io.IO
import scala.io.StdIn

class Watcher extends Actor with ActorLogging {
  import context._

  val ports = List(
    "/dev/ttyUSB\\d+",
    "/dev/ttyACM\\d+",
    "/dev/cu\\d+",
    "/dev/ttyS\\d+"
  )

  override def preStart() = {
    val cmd = Serial.Watch()
    IO(Serial) ! cmd //watch for new devices
    log.info(s"Watching ${cmd.directory} for new devices.")
  }

  def receive = {

    case Serial.CommandFailed(w: Serial.Watch, err) =>
      log.error(err, s"Could not get a watch on ${w.directory}.")
      context stop self

    case Serial.Connected(path) =>
      log.info(s"New device: ${path}")
      ports.find(path matches _) match {
        case Some(port) => log.info(s"Device is a serial device.")
        case None => log.warning(s"Device is NOT serial device.")
      }

  }

}

object Main {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("flow")
    val watcher = system.actorOf(Props(classOf[Watcher]), name = "watcher")
    StdIn.readLine()
    system.shutdown()
  }

}
