package com.github.jodersky.flow
package stream
package impl

import akka.actor._
import akka.stream.stage.GraphStageLogic.StageActorRef
import akka.util._
import akka.stream._
import akka.stream.stage._
import scala.concurrent._
import akka.io._
import com.github.jodersky.flow.{Serial => CoreSerial}
import akka.stream.impl.ReactiveStreamsCompliance


/**
  * Graph logic that handles establishing and forwarding serial communication.
  * The underlying stream is closed when downstream (output) finishes,
  * conversely upstream (input) closes are ignored.
  */
private[stream] class SerialConnectionLogic(
  shape: FlowShape[ByteString, ByteString],
  manager: ActorRef,
  port: String,
  settings: SerialSettings,
  failOnOverflow: Boolean,
  bufferSize: Int,
  connectionPromise: Promise[Serial.Connection])
    extends GraphStageLogic(shape) {
  import GraphStageLogic._
  import SerialConnectionLogic._

  /** Receives data and writes it to the serial backend. */
  private def in: Inlet[ByteString] = shape.in

  /** Receives data from the serial backend and pushes it downstream. */
  private def out: Outlet[ByteString] = shape.out

  /** Implicit alias to stageActor so it will be used in "!" calls, without
    * explicitly specifying a sender. */
  implicit private def self = stageActor.ref

  /** Input handler for an established connection.
    * @param operator ther operator actor of the established connection
    */
  class ConnectedInHandler(operator: ActorRef) extends InHandler {

    override def onPush(): Unit = {
      val elem = grab(in)
      require(elem != null) // reactive streams requirement
      operator ! CoreSerial.Write(elem, _ => WriteAck)
    }

    override def onUpstreamFinish(): Unit = {
      if (isClosed(out)) { // close serial connection if output is also closed
        operator ! CoreSerial.Close
      }
    }

  }

  class ConnectedOutHandler(operator: ActorRef) extends OutHandler {
    // alias stage actor as implicit to it will be used in "!" calls
    implicit val self = stageActor.ref

    override def onPull(): Unit = {
      // serial connections are at the end of the "backpressure chain",
      // they do not natively support backpressure (as does TCP for example)
      // therefore nothing is done here as
    }

    override def onDownstreamFinish(): Unit = {
      // closing downstream also closes the underlying connection
      operator ! CoreSerial.Close
    }

  }

  override def preStart(): Unit = {
    setKeepGoing(true) // serial connection operator will manage completing stage
    getStageActor(connecting)
    stageActor watch manager
    manager ! CoreSerial.Open(port, settings, bufferSize)
  }

  setHandler(in, IgnoreTerminateInput)
  setHandler(out, IgnoreTerminateOutput)

  /** Initial behaviour, before a serial connection is established. */
  private def connecting(event: (ActorRef, Any)): Unit = {
    val sender = event._1
    val message = event._2

    message match {

      case Terminated(`manager`) =>
        val ex = new StreamSerialException("The IO manager actor (Serial) has terminated. Stopping now.")
        failStage(ex)
        connectionPromise.failure(ex)

      case CoreSerial.CommandFailed(cmd, reason) =>
        val ex = new StreamSerialException(s"Serial command [$cmd] failed", reason)
        failStage(ex)
        connectionPromise.failure(ex)

      case CoreSerial.Opened(port) =>
        val operator = sender
        setHandler(in, new ConnectedInHandler(operator))
        setHandler(out, new ConnectedOutHandler(operator))
        stageActor become connected(operator)
        connectionPromise.success(Serial.Connection(port, settings)) //complete materialized value
        stageActor unwatch manager
        stageActor watch operator
        pull(in) // start pulling input

      case other =>
        val ex = new StreamSerialException(s"Stage actor received unkown message [$other]")
        failStage(ex)
        connectionPromise.failure(ex)

    }

  }

  /** Behaviour once a connection has been established. It is assumed that operator is not null. */
  private def connected(operator: ActorRef)(event: (ActorRef, Any)): Unit = {
    val sender = event._1
    val message = event._2

    message match {

      case Terminated(`operator`) =>
        failStage(new StreamSerialException("The connection actor has terminated. Stopping now."))

      case CoreSerial.CommandFailed(cmd, reason) =>
        failStage(new StreamSerialException(s"Serial command [$cmd] failed.", reason))

      case CoreSerial.Closed =>
        completeStage()

      case CoreSerial.Received(data) =>
        if (isAvailable(out)) {
          push(out, data)
        } else if (failOnOverflow) {
          /* Note that the native backend does not provide any way of informing about
           * dropped serial data. However, in most cases, a computer capable of running flow
           * is also capable of processing incoming serial data at typical baud rates.
           * Hence packets will usually only be dropped if an application that uses flow
           * backpressures, which can however be detected here. */
          failStage(new StreamSerialException("Incoming serial data was dropped."))
        }

      case WriteAck =>
        if (!isClosed(in)) {
          pull(in)
        }

      case other =>
        failStage(new StreamSerialException(s"Stage actor received unkown message [$other]"))

    }

  }

}

private[stream] object SerialConnectionLogic {

  case object WriteAck extends CoreSerial.Event

}
