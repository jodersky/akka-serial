package ch.jodersky.flow

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a serial connection in a more secure and object-oriented style than `UnsafeSerial`. In
 * contrast to the latter, this class encapsulates and secures any pointers used to communicate with
 * the native backend and is thread-safe.
 *
 * The underlying serial port is assumed open when this class is initialized.
 */
private[flow] class SerialConnection private (
  unsafe: UnsafeSerial,
  val port: String
) {

  private var reading: Boolean = false
  private val readLock = new Object

  private var writing: Boolean = false
  private val writeLock = new Object

  private val closed = new AtomicBoolean(false)

  /**
   * Checks if this serial port is closed.
   */
  def isClosed = closed.get()

  /**
   * Closes the underlying serial connection. Any callers blocked on read or write will return.
   * A call of this method has no effect if the serial port is already closed.
   * @throws IOException on IO error
   */
  def close(): Unit = this.synchronized {
    if (!closed.get) {
      closed.set(true)
      unsafe.cancelRead()
      readLock.synchronized {
        while (reading) this.wait()
      }
      writeLock.synchronized {
        while (writing) this.wait()
      }
      unsafe.close()
    }
  }

  /**
   * Reads data from underlying serial connection into a ByteBuffer.
   * Note that data is read into the buffer's memory, its attributes
   * such as position and limit are not modified.
   *
   * A call to this method is blocking, however it is interrupted
   * if the connection is closed.
   *
   * This method works for direct and indirect buffers but is optimized
   * for the former.
   *
   * @param buffer a ByteBuffer into which data is read
   * @return the actual number of bytes read
   * @throws PortInterruptedException if port is closed while reading
   * @throws IOException on IO error
   */
  def read(buffer: ByteBuffer): Int = readLock.synchronized {
    if (!closed.get) {
      try {
        reading = true
        unsafe.read(buffer)
      } finally {
        reading = false
        if (closed.get) readLock.notify()
      }
    } else {
      throw new PortClosedException(s"${port} is closed")
    }
  }

  /**
   * Writes data from a ByteBuffer to underlying serial connection.
   * Note that data is read from the buffer's memory, its attributes
   * such as position and limit are not modified.
   *
   * The write is non-blocking, this function returns as soon as the data is copied into the kernel's
   * transmission buffer.
   *
   * This method works for direct and indirect buffers but is optimized
   * for the former.
   *
   * @param buffer a ByteBuffer from which data is taken
   * @return the actual number of bytes written
   * @throws IOException on IO error
   */
  def write(buffer: ByteBuffer): Int = writeLock.synchronized {
    if (!closed.get) {
      try {
        writing = true
        unsafe.write(buffer, buffer.position)
      } finally {
        writing = false
        if (closed.get) writeLock.notify()
      }
    } else {
      throw new PortClosedException(s"${port} is closed")
    }
  }

}

private[flow] object SerialConnection {

  /**
   * Opens a new connection to a serial port.
   * This method acts as a factory to creating serial connections.
   *
   * @param port name of serial port to open
   * @param settings settings with which to initialize the connection
   * @return an instance of the open serial connection
   * @throws NoSuchPortException if the given port does not exist
   * @throws AccessDeniedException if permissions of the current user are not sufficient to open port
   * @throws PortInUseException if port is already in use
   * @throws InvalidSettingsException if any of the specified settings are invalid
   * @throws IOException on IO error
   */
  def open(
    port: String,
    settings: SerialSettings
  ): SerialConnection = synchronized {
    val pointer = UnsafeSerial.open(
      port,
      settings.baud,
      settings.characterSize,
      settings.twoStopBits,
      settings.parity.id
    )
    new SerialConnection(new UnsafeSerial(pointer), port)
  }

}
