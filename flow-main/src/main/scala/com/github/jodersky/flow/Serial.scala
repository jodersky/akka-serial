package com.github.jodersky.flow

import akka.actor.ExtensionKey
import akka.util.ByteString

/** Defines messages used by flow's serial IO layer. */
object Serial extends ExtensionKey[SerialExt] {

  /** Base trait for any flow-related messages. */
  sealed trait Message

  /** A message extending this trait is to be viewed as a command, an out-bound message issued by the client to flow's API. */
  trait Command extends Message

  /** A message extending this trait is to be viewed as an event, an in-bound message issued by flow to the client. */
  trait Event extends Message

  /** A command has failed. */
  case class CommandFailed(command: Command, reason: Throwable) extends Event

  /**
   * Open a new serial port.
   *
   * Send this command to the serial manager to request the opening of a serial port. The manager will
   * attempt to open a serial port with the specified parameters and, if successful, create a `SerialOperator` actor associated to the port.
   * The operator actor acts as an intermediate to the underlying native serial port, dealing with threading issues and dispatching messages.
   *
   * In case the port is successfully opened, the operator will respond with an `Opened` message.
   * In case the port cannot be opened, the manager will respond with a `CommandFailed` message.
   *
   * @param port name of serial port to open
   * @param settings settings of serial port to open
   * @param bufferSize maximum read and write buffer sizes
   */
  case class Open(port: String, settings: SerialSettings, bufferSize: Int = 1024) extends Command

  /**
   * A port has been successfully opened.
   *
   * Event sent by a port operator, indicating that a serial port was successfully opened. The sender
   * of this message is the operator associated to the given serial port.
   *
   * @param port name of opened serial port
   */
  case class Opened(port: String) extends Event

  /**
   * Data has been received.
   *
   * Event sent by an operator, indicating that data was received on the operator's serial port.
   *
   * @param data data received on the port
   */
  case class Received(data: ByteString) extends Event

  /**
   * Write data to a serial port.
   *
   * Send this command to an operator to write the given data to its associated serial port.
   * An acknowledgment may be set, in which case it is sent back to the sender on a successful write.
   * Note that a successful write does not guarantee the actual transmission of data through the serial port,
   * it merely guarantees that the data has been stored in the operating system's kernel buffer, ready to
   * be transmitted.
   *
   * @param data data to be written to port
   * @param ack acknowledgment sent back to sender once data has been enqueued in kernel for sending (the acknowledgment
   * is a function 'number of bytes written => event')
   */
  case class Write(data: ByteString, ack: Int => Event = NoAck) extends Command

  /**
   *  Special type of acknowledgment that is not sent back.
   */
  case object NoAck extends Function1[Int, Event] {
    def apply(length: Int) = sys.error("cannot apply NoAck")
  }

  /**
   *  Request closing of port.
   *
   *  Send this command to an operator to close its associated port. The operator will respond
   *  with a `Closed` message upon closing the serial port.
   */
  case object Close extends Command

  /**
   * A port has been closed.
   *
   * Event sent from operator, indicating that its port has been closed.
   */
  case object Closed extends Event

  /**
   * Watch a directory for new ports.
   *
   * Send this command to the manager to get notifications when a new port (i.e. file) is created in
   * the given directory.
   * In case the given directory cannot be watched, the manager responds with a `CommandFailed` message.
   *
   * Note: the directory must exist when this message is sent.
   *
   * @param directory the directory to watch
   *
   * @see Unwatch
   * @see Connected
   */
  case class Watch(directory: String = "/dev") extends Command

  /**
   * Stop receiving notifications about a previously watched directory.
   *
   * @param directory the directory to unwatch
   */
  case class Unwatch(directory: String = "/dev") extends Command

  /**
   * A new port (i.e. file) has been detected.
   *
   * @param port the absolute file name of the connected port
   */
  case class Connected(port: String) extends Event

}
