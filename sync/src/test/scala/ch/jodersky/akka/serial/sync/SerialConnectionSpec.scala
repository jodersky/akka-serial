package ch.jodersky.akka.serial
package sync

import java.nio.ByteBuffer
import org.scalatest._

class SerialConnectionSpec extends WordSpec with PseudoTerminal {

  def withEchoConnection[A](action: SerialConnection => A): A = {
    withEcho { (port, settings) =>
      val connection = SerialConnection.open(port, settings)
      try {
        action(connection)
      } finally {
        connection.close()
      }
    }
  }

  "A SerialConnection" should {

    "open a valid port" in {
      withEcho { (port, settings) =>
        SerialConnection.open(port, settings)
      }
    }

    "throw an exception on an invalid port" in {
      val settings = SerialSettings(baud = 115200)
      intercept[NoSuchPortException] {
        SerialConnection.open("/dev/nonexistant", settings)
      }
    }

    "read the same data it writes to an echo pty" in {
      withEchoConnection { conn =>
        /* Note: this test assumes that all data will be written and read
         * within single write and read calls. This in turn assumes that
         * internal operating system buffers have enough capacity to
         * store all data. */
        val bufferSize = 64

        val outString = "hello world"
        val outBuffer = ByteBuffer.allocateDirect(bufferSize)
        val outData = outString.getBytes
        outBuffer.put(outData)
        conn.write(outBuffer)

        val inBuffer = ByteBuffer.allocateDirect(bufferSize)
        conn.read(inBuffer)
        val inData = new Array[Byte](inBuffer.remaining())
        inBuffer.get(inData)
        val inString = new String(inData)

        assert(inString == outString)
      }
    }

    "interrupt a read when closing a port" in {
      withEchoConnection { conn =>
        val buffer = ByteBuffer.allocateDirect(64)

        val closer = new Thread {
          override def run(): Unit = {
            Thread.sleep(100)
            conn.close()
          }
        }
        closer.start()
        intercept[PortInterruptedException]{
          conn.read(buffer)
        }
        closer.join()
      }
    }

    "throw an exception when reading from a closed port" in {
      withEchoConnection { conn =>
        val buffer = ByteBuffer.allocateDirect(64)
        conn.close()

        intercept[PortClosedException]{
          conn.read(buffer)
        }
      }
    }

    "throw an exception when writing to a closed port" in {
      withEchoConnection { conn =>
        val buffer = ByteBuffer.allocateDirect(64)
        conn.close()

        intercept[PortClosedException]{
          conn.write(buffer)
        }
      }
    }
        
  }

}
