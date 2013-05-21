package com.github.jodersky.flow

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import java.io.IOException

class Serial private (val port: String, private val pointer: Long, reader: Array[Byte] => Unit) {
  future {
    var n = 0
    val buffer = new Array[Byte](100)
    while (n >= 0) {
      n = NativeSerial.read(pointer, buffer)
      import NativeSerial._
      n match {
          case E_POINTER => throw new NullPointerException("pointer to native serial")
          case E_POLL => throw new IOException(port + ": polling")
          case E_IO => throw new IOException(port + ": reading")
          case E_CLOSE => println("close request")
          case x if x > 0 => reader(buffer.take(n))
      }
    }
  }

  private def writeBlock(data: Array[Byte]) = synchronized {
    import NativeSerial._
    NativeSerial.write(pointer, data) match {
      case E_POINTER => throw new NullPointerException("pointer to native serial")
      case E_IO => throw new IOException(port + ": writing")
      case r => data.take(r) 
    }
  }
  
  def write(data: Array[Byte]) = future { writeBlock(data) }

  def close() = synchronized {
    NativeSerial.close(pointer)
  }

}

object Serial {

  def open(port: String, baud: Int)(reader: Array[Byte] => Unit) = synchronized {
    val pointer = new Array[Long](1)
    val result = NativeSerial.open(port, baud, pointer)

    import NativeSerial._
    result match {
      case E_PERMISSION => throw new AccessDeniedException(port)
      case E_OPEN => throw new NoSuchPortException(port)
      case E_BUSY => throw new PortInUseException(port)
      case E_BAUD => throw new IllegalArgumentException(
        s"invalid baudrate ${baud}, use standard unix values")
      case E_PIPE => throw new IOException("cannot create pipe")
      case E_MALLOC => throw new IOException("cannot allocate memory")
      case 0 => new Serial(port, pointer(0), reader)
      case _ => throw new Exception("cannot open port")
    }
  }
  
  def debug(value: Boolean) = NativeSerial.debug(value)

}