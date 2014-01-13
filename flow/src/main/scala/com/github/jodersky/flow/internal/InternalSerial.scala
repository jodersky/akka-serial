package com.github.jodersky.flow.internal

import java.io.IOException
import com.github.jodersky.flow._
import java.util.concurrent.atomic.AtomicBoolean

/** Wraps `NativeSerial` in a more object-oriented style, still quite low level. */
class InternalSerial private (val port: String, val baud: Int, val characterSize: Int, val twoStopBits: Boolean, val parity: Int, private val pointer: Long) {
  import InternalSerial._

  private val reading = new AtomicBoolean(false)
  private val writing = new AtomicBoolean(false)
  private val closed = new AtomicBoolean(false)

  /** Closes the underlying serial connection. Any threads blocking on read or write will return. */
  def close(): Unit = synchronized {
    if (!closed.get()) {
      closed.set(true)
      except(NativeSerial.interrupt(pointer), port)
      if (writing.get()) wait()
      if (reading.get()) wait()
      except(NativeSerial.close(pointer), port)
    }
  }

  /**
   * Read data from underlying serial connection.
   * @throws PortInterruptedException if port is closed from another thread
   */
  def read(): Array[Byte] = if (!closed.get) {
    reading.set(true)
    try {
      val buffer = new Array[Byte](100)
      val bytesRead = except(NativeSerial.read(pointer, buffer), port)
      buffer take bytesRead
    } finally {
      synchronized {
        reading.set(false)
        if (closed.get) notify()
      }
    }
  } else {
    throw new PortClosedException(s"port ${port} is already closed")
  }

  /**
   * Write data to underlying serial connection.
   * @throws PortInterruptedException if port is closed from another thread
   */
  def write(data: Array[Byte]): Array[Byte] = if (!closed.get) {
    writing.set(true)
    try {
      val bytesWritten = except(NativeSerial.write(pointer, data), port)
      data take bytesWritten
    } finally {
      synchronized {
        writing.set(false)
        if (closed.get) notify()
      }
    }
  } else {
    throw new PortClosedException(s"port ${port} is already closed")
  }

}

object InternalSerial {
  import NativeSerial._

  /** Transform error code to exception if necessary. */
  private def except(result: Int, port: String): Int = result match {
    case E_IO => throw new IOException(port)
    case E_ACCESS_DENIED => throw new AccessDeniedException(port)
    case E_BUSY => throw new PortInUseException(port)
    case E_INVALID_SETTINGS => throw new InvalidSettingsException("the provided settings are invalid: be sure to use standard baud rate, character size and parity.")
    case E_INTERRUPT => throw new PortInterruptedException(port)
    case E_NO_PORT => throw new NoSuchPortException(port)
    case error if error < 0 => throw new IOException(s"unknown error code: ${error}")
    case success => success
  }

  /** Open a new connection to a serial port. */
  def open(port: String, baud: Int, characterSize: Int, twoStopBits: Boolean, parity: Int): InternalSerial = synchronized {
    val pointer = new Array[Long](1)
    except(NativeSerial.open(port, baud, characterSize, twoStopBits, parity, pointer), port)
    new InternalSerial(port, baud, characterSize, twoStopBits, parity, pointer(0))
  }

  /** Set debugging for all serial connections. Debugging results in printing extra messages from the native library in case of errors. */
  def debug(value: Boolean) = NativeSerial.debug(value)

}