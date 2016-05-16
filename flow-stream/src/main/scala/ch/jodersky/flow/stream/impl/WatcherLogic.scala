package ch.jodersky.flow
package stream
package impl

import scala.concurrent.Promise

import akka.actor.{ActorRef, Terminated}
import akka.stream.SourceShape
import akka.stream.stage.GraphStageLogic
import ch.jodersky.flow.{Serial => CoreSerial}

private[stream] class WatcherLogic(
  shape: SourceShape[String],
  ioManager: ActorRef,
  ports: Set[String],
  watchPromise: Promise[Serial.Watch])
    extends GraphStageLogic(shape) {
  import GraphStageLogic._

  implicit private def self = stageActor.ref

  override def preStart(): Unit = {
    getStageActor(receive)
    stageActor watch ioManager
    for (dir <- WatcherLogic.getDirs(ports)) {
      ioManager ! CoreSerial.Watch(dir, skipInitial = false)
    }
  }

  setHandler(shape.out, IgnoreTerminateOutput)

  private def receive(event: (ActorRef, Any)): Unit = {
    val sender = event._1
    val message = event._2

    message match {

      case Terminated(`ioManager`) =>
        val ex = new StreamWatcherException("The serial IO manager has terminated. Stopping now.")
        failStage(ex)
        watchPromise.failure(ex)

      case CoreSerial.CommandFailed(cmd, reason) =>
        val ex = new StreamWatcherException(s"Serial command [$cmd] failed", reason)
        failStage(ex)
        watchPromise.failure(ex)

      case CoreSerial.Connected(port) =>
        if (ports contains port) {
          if (isAvailable(shape.out)) {
            push(shape.out, port)
          }
        }

      case other =>
        failStage(new StreamWatcherException(s"Stage actor received unkown message [$other]"))

    }
  }

}

private[stream] object WatcherLogic {
  def getDirs(ports: Set[String]): Set[String] = ports.map(_.split("/").init.mkString("/"))
}
