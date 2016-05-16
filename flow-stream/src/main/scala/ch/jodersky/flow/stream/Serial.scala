package ch.jodersky.flow
package stream

import akka.stream.scaladsl.Source
import scala.concurrent.Future

import akka.actor.{Extension, ActorSystem, ExtendedActorSystem, ExtensionId, ExtensionIdProvider}
import akka.io.IO
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import ch.jodersky.flow.{Serial => CoreSerial}
import impl._

object Serial extends ExtensionId[Serial] with ExtensionIdProvider {

  /**
    * Represents a prospective serial connection.
    */
  case class Connection(port: String, settings: SerialSettings)

  case class Watch(ports: Set[String])

  def apply()(implicit system: ActorSystem): Serial = super.apply(system)

  override def lookup() = Serial

  override def createExtension(system: ExtendedActorSystem): Serial = new Serial(system)

}

/**
  * Entry point to streaming over serial ports.
  * The design of this API is inspired by Akka's Tcp streams.
  */
class Serial(system: ExtendedActorSystem) extends Extension {

  /**
    * Creates a Flow that will open a serial port when materialized.
    * This Flow then represents an open serial connection: data pushed to its
    * inlet will be written to the underlying serial port, and data received
    * on the port will be emitted by its outlet.
    * @param port name of serial port to open
    * @param settings settings to use with serial port
    * @param failOnOverflow when set, the returned Flow will fail when incoming data is dropped
    * @param bufferSize maximum read and write buffer sizes
    * @return a Flow associated to the given serial port
    */
  def open(port: String, settings: SerialSettings, failOnOverflow: Boolean = false, bufferSize: Int = 1024):
      Flow[ByteString, ByteString, Future[Serial.Connection]] = Flow.fromGraph(
    new SerialConnectionStage(
      IO(CoreSerial)(system),
      port,
      settings,
      failOnOverflow,
      bufferSize
    )
  )

  def watch(ports: Set[String]): Source[String, Future[Serial.Watch]] = Source.fromGraph(
    new WatcherStage(
      IO(CoreSerial)(system),
      ports
    )
  )

}
