package com.github.jodersky.flow.low

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import java.io.IOException
import com.github.jodersky.flow.AccessDeniedException
import com.github.jodersky.flow.PortInUseException
import com.github.jodersky.flow.PortClosedException
import com.github.jodersky.flow.IllegalBaudRateException
import scala.util.Try
import java.util.concurrent.atomic.AtomicBoolean
import com.github.jodersky.flow.PortInterruptedException
import com.github.jodersky.flow.NoSuchPortException

class Serial private (val port: String, private val pointer: Long) {
  import Serial._

  private val reading = new AtomicBoolean(false)
  private val writing = new AtomicBoolean(false)
  private val closed = new AtomicBoolean(false)

  def close(): Unit = synchronized {
    if (!closed.get()) {
      closed.set(true)
      except(NativeSerial.interrupt(pointer), port)
      if (writing.get()) wait() // if reading, wait for read to finish
      if (reading.get()) wait()
      except(NativeSerial.close(pointer), port)
    }
  }

  def read(): Array[Byte] = if (!closed.get) {
    reading.set(true)
    try {
      val buffer = new Array[Byte](100)
      val bytesRead = except(NativeSerial.read(pointer, buffer), port)
      buffer take bytesRead
    } finally {
      synchronized {
        reading.set(false)
        if (closed.get) notify(); //read was interrupted by close
      }
    }
  } else {
    throw new PortClosedException(s"port ${port} is already closed")
  }

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

object Serial {
  import NativeSerial._

  private def except(result: Int, port: String): Int = result match {
    case E_IO => throw new IOException(port)
    case E_ACCESS_DENIED => throw new AccessDeniedException(port)
    case E_BUSY => throw new PortInUseException(port)
    case E_INVALID_BAUD => throw new IllegalBaudRateException("use standard baud rate")
    case E_INTERRUPT => throw new PortInterruptedException(port)
    case E_NO_PORT => throw new NoSuchPortException(port)
    case error if error < 0 => throw new IOException(s"unknown error code: ${error}")
    case success => success
  }

  def open(port: String, baud: Int): Serial = synchronized {
    val pointer = new Array[Long](1)
    except(NativeSerial.open(port, baud, pointer), port)
    new Serial(port, pointer(0))
  }

  def debug(value: Boolean) = NativeSerial.debug(value)

}