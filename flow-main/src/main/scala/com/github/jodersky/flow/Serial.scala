package com.github.jodersky.flow

import akka.actor.ActorRef
import akka.actor.ExtensionKey
import akka.util.ByteString

/** Defines messages used by serial IO layer. */
object Serial extends ExtensionKey[SerialExt] {

  /** A message extending this trait is to be viewed as a command, that is an outbound message. */
  trait Command

  /** A message extending this trait is to be viewed as an event, that is an inbound message. */
  trait Event

  /**
   * Open a new serial port. Send this command to the serial manager to request the opening of a new port.
   * @param handler actor that will receive events from the specified serial port
   * @param port name of serial port
   * @param baud baud rate to use with serial port
   * @param characterSize size of a character of the data sent through the serial port
   * @param twoStopBits set to use two stop bits instead of one
   * @param parity type of parity to use with serial port
   */
  case class Open(handler: ActorRef, port: String, baud: Int, characterSize: Int = 8, twoStopBits: Boolean = false, parity: Parity.Parity = Parity.None) extends Command

  /**
   * Event sent from a port operator, indicating that a serial port was successfully opened.
   * @param port name of serial port
   * @param baud baud rate to use with serial port
   * @param characterSize size of a character of the data sent through the serial port
   * @param twoStopBits set to use two stop bits instead of one
   * @param parity type of parity to use with serial port
   */
  case class Opened(port: String, baud: Int, characterSize: Int, twoStopBits: Boolean, parity: Parity.Parity) extends Event

  /**
   * Event sent from manager, indicating that a serial port could not be opened.
   * @param reason throwable containing reason to why the requested port could not be opened
   * @param port name of serial port
   * @param baud baud rate to use with serial port
   * @param characterSize size of a character of the data sent through the serial port
   * @param twoStopBits set to use two stop bits instead of one
   * @param parity type of parity to use with serial port
   */
  case class OpenFailed(reason: Throwable, port: String, baud: Int, characterSize: Int, twoStopBits: Boolean, parity: Parity.Parity) extends Event

  /**
   * Event sent by operator, indicating that data was received from the operator's serial port.
   * @param data data received by port
   */
  case class Received(data: ByteString) extends Event

  /**
   * Command sent to operator, requesting the writing of data to the operator's serial port. Optionally request acknowledgment when data is written.
   * @param data data to be written to port
   * @param ack set to true to receive acknowledgment on successful write
   * @see Wrote
   */
  case class Write(data: ByteString, ack: Boolean = false) extends Command

  /**
   * Event sent by operator, acknowledging that data was written to its serial port. Note that such an acknowledgment only guarantees that data has been written
   * to the serial port's output buffer (managed by the operating system), the actual reception of the data on the remote device is not guaranteed.
   * @param data the data that has been written
   */
  case class Wrote(data: ByteString) extends Event

  /** Command sent to operator to request the closing of its serial port. */
  case object Close extends Command

  /**
   * Event sent from operator, indicating that its port has been closed. An optional reason explains the error that caused the closing of the port.
   * @param reason Some(exception) explaining the exception that caused the closing, None if the port was closed by sending a `Close` message.
   */
  case class Closed(reason: Option[Exception]) extends Event

}
