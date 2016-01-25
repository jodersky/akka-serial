package com.github.jodersky.flow
package stream
package impl

import akka.actor._
import akka.util._
import akka.stream._
import akka.stream.stage._
import scala.concurrent._

/**
  * Graph stage that establishes and thereby materializes a serial connection.
  * The actual connection logic is deferred to [[SerialConnectionLogic]].
  */
private[stream] class SerialConnectionStage(
  manager: ActorRef,
  port: String,
  settings: SerialSettings,
  failOnOverflow: Boolean,
  bufferSize: Int
) extends GraphStageWithMaterializedValue[FlowShape[ByteString, ByteString], Future[Serial.Connection]] {

  val in: Inlet[ByteString] = Inlet("Serial.in")
  val out: Outlet[ByteString] = Outlet("Serial.out")

  val shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes):
      (GraphStageLogic, Future[Serial.Connection]) = {

    val connectionPromise = Promise[Serial.Connection]

    val logic = new SerialConnectionLogic(
      shape,
      manager,
      port,
      settings,
      failOnOverflow,
      bufferSize,
      connectionPromise
    )

    (logic, connectionPromise.future)
  }

  override def toString = s"Serial($port)"

}
