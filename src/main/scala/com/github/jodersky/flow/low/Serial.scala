package com.github.jodersky.flow.low

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import java.io.IOException
import com.github.jodersky.flow.AccessDeniedException
import com.github.jodersky.flow.NoSuchPortException
import com.github.jodersky.flow.PortInUseException
import com.github.jodersky.flow.PortClosingException
import com.github.jodersky.flow.IllegalBaudRateException
import scala.util.Try


class Serial private (val port: String, private val pointer: Long) {
  import NativeSerial._
  
  /** State of current connection, required so that close may not be called multiple
   *  times and used to ensure close and read are never called at the same moment. */
  private var _closed = false;
  private def closed = synchronized{_closed}
  private def closed_=(newValue: Boolean) = synchronized{_closed = newValue} 
  

  def read(): Array[Byte] = {
    if (!closed) {
      //read
    } else {
      //
    }
    
    val buffer = new Array[Byte](100)
    
    NativeSerial.read(pointer, buffer) match {
      case E_INTERRUPT => throw new PortClosingException(port)
      case bytes if bytes > 0 => buffer.take(bytes)
      case error => throw new IOException(s"unknown error code: ${error}")
    }
  }

  def write(data: Array[Byte]): Array[Byte] = {
    //
    import NativeSerial._
    NativeSerial.write(pointer, data) match {
      case E_IO => throw new IOException(port)
      case bytes if bytes > 0 => data.take(bytes)
      case error => throw new IOException(s"unknown write error ${error}")
    }
  }

  def close() = {
    NativeSerial.close(pointer)
  }

}

object Serial {
  import NativeSerial._

  def except(result: Int): Int = result match {
    case E_IO => throw new IOException("")
    case E_ACCESS_DENIED => 0
    case E_BUSY => 0
    case E_INVALID_BAUD => 0
    case E_INTERRUPT => 0
  }

  def open(port: String, baud: Int): Serial = synchronized {
    val pointer = new Array[Long](1)
    NativeSerial.open(port, baud, pointer) match {
      case E_IO => throw new IOException("")
      case E_ACCESS_DENIED => throw new AccessDeniedException(port)
      case E_BUSY => throw new PortInUseException(port)
      case E_INVALID_BAUD => throw new IllegalBaudRateException(baud.toString)
      //handle no such port
      case 0 => new Serial(port, pointer(0))
      case error => throw new IOException(s"unknown error code: ${error}")
    }
  }

  def debug(value: Boolean) = NativeSerial.debug(value)

}