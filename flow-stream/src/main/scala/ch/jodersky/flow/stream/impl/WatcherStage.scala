package ch.jodersky.flow
package stream
package impl

import scala.concurrent.{Future, Promise}

import akka.actor.ActorRef
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStageWithMaterializedValue, GraphStageLogic}


private[stream] class WatcherStage(
  ioManager: ActorRef,
  ports: Set[String]
) extends GraphStageWithMaterializedValue[SourceShape[String], Future[Serial.Watch]] {

  val out = Outlet[String]("Watcher.out")

  val shape = new SourceShape(out)

  override def createLogicAndMaterializedValue(attributes: Attributes):
      (GraphStageLogic, Future[Serial.Watch]) = {

    val promise = Promise[Serial.Watch]

    val logic = new WatcherLogic(
      shape,
      ioManager,
      ports,
      promise
    )

    (logic, promise.future)
  }

  override def toString = s"Watcher($ports)"

}
