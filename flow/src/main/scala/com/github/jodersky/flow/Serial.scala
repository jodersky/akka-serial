package com.github.jodersky.flow

import akka.actor.ActorRef
import akka.actor.ExtensionKey
import akka.util.ByteString

/** Defines messages used by flow's serial IO layer. */
object Serial extends ExtensionKey[SerialExt] {

  /** A message extending this trait is to be viewed as a command, an out-bound message issued by the client to flow's API. */
  trait Command

  /** A message extending this trait is to be viewed as an event, an in-bound message issued by flow to the client. */
  trait Event

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
   * @param settings settings of serial port to open
   */
  case class Open(settings: SerialSettings) extends Command

  /**
   * A port has been successfully opened.
   *
   * Event sent by a port operator, indicating that a serial port was successfully opened. The sender
   * of this message is the operator associated to the given serial port. Furthermore, an additional reference
   * to the operator is provided in this class' `operator` field.
   *
   * @param settings settings of port that was opened
   * @param operator operator associated with the serial port
   */
  case class Opened(settings: SerialSettings, operator: ActorRef) extends Event

  /**
   * Register an actor to receive events.
   *
   * Send this command to a serial operator to register an actor for notification on the reception of data on the operator's associated port.
   * Upon reception, data will be sent by the operator to registered actors in form of `Received` events.
   *
   * @param receiver actor to register
   */
  case class Register(receiver: ActorRef) extends Command

  /**
   * Unregister an actor from receiving events.
   *
   * Send this command to a serial operator to unregister an actor for notification on the reception of data on the operator's associated port.
   *
   * @param receiver actor to unregister
   */
  case class Unregister(receiver: ActorRef) extends Command

  /**
   * Data has been received.
   *
   * Event sent by an operator, indicating that data was received on the operator's serial port.
   * Clients must register (see `Register`) with a serial operator to receive these events.
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
   * be written.
   *
   * @param data data to be written to port
   * @param ack acknowledgment sent back to sender once data has been enqueued in kernel for sending
   */
  case class Write(data: ByteString, ack: Event = NoAck) extends Command

  /**
   *  Special type of acknowledgment that is not sent back.
   */
  case object NoAck extends Event

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

}
