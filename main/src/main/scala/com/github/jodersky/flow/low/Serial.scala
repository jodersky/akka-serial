package com.github.jodersky.flow.low

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import java.io.IOException
import com.github.jodersky.flow.AccessDeniedException
import com.github.jodersky.flow.NoSuchPortException
import com.github.jodersky.flow.PortInUseException
import com.github.jodersky.flow.PortClosingException
import scala.util.Try

class Serial private (val port: String, private val pointer: Long) {
  import NativeSerial._

  def read(): Array[Byte] = synchronized {
    val buffer = new Array[Byte](100)
    NativeSerial.read(pointer, buffer) match {
      case E_POINTER => throw new NullPointerException("pointer to native serial")
      case E_POLL => throw new IOException(port + ": polling")
      case E_IO => throw new IOException(port + ": reading")
      case E_CLOSE => throw new PortClosingException(port + " closing")
      case bytes if bytes > 0 => buffer.take(bytes)
      case error => throw new IOException(s"unknown read error ${error}")
    }
  }

  def write(data: Array[Byte]): Array[Byte] = {
    import NativeSerial._
    NativeSerial.write(pointer, data) match {
      case E_POINTER => throw new NullPointerException("pointer to native serial")
      case E_IO => throw new IOException(port + ": writing")
      case bytes if bytes > 0 => data.take(bytes)
      case error => throw new IOException(s"unknown write error ${error}")
    }
  }

  def close() = {
    NativeSerial.close(pointer)
  }

}

object Serial {

  def open(port: String, baud: Int) = synchronized {
    val pointer = new Array[Long](1)
    val result = NativeSerial.open(port, baud, pointer)

    import NativeSerial._

    result match {
      case E_PERMISSION => throw new AccessDeniedException(port)
      case E_OPEN => throw new NoSuchPortException(port)
      case E_BUSY => throw new PortInUseException(port)
      case E_BAUD => throw new IllegalArgumentException(s"invalid baudrate ${baud}, use standard unix values")
      case E_PIPE => throw new IOException("cannot create pipe")
      case E_MALLOC => throw new IOException("cannot allocate memory for serial port")
      case 0 => new Serial(port, pointer(0))
      case error => throw new IOException(s"unknown error ${error}")
    }
  }

  def debug(value: Boolean) = NativeSerial.debug(value)

}