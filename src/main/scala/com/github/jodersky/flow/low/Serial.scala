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
import java.nio.channels.InterruptibleChannel

class Serial private (val port: String, private val pointer: Long) extends InterruptibleChannel {
  import Serial._

  private val reading = new AtomicBoolean(false)
  private val writing = new AtomicBoolean(false)
  private val closed = new AtomicBoolean(false)
  
  def isOpen = !closed.get

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
    val buffer = new Array[Byte](100)
    val readResult = NativeSerial.read(pointer, buffer)
    synchronized {
      reading.set(false)
      if (closed.get) notify(); //read was interrupted by close
    }
    val n = except(readResult, port)
    buffer take n
  } else {
    throw new PortClosedException(s"port ${port} is already closed")
  }

  def write(data: Array[Byte]): Array[Byte] = if (!closed.get) {
    writing.set(true)
    val writeResult = NativeSerial.write(pointer, data)
    synchronized {
      writing.set(false)
      if (closed.get) notify()
    }
    val n = except(writeResult, port)
    data take n
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