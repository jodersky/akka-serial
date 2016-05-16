package ch.jodersky.flow

import java.nio.ByteBuffer

import ch.jodersky.jni.nativeLoader

/**
  * Low-level wrapper of native serial backend.
  *
  * WARNING: Methods in this class allocate native structures and deal with pointers.  These
  * pointers are handled as longs by java and are NOT checked for correctness, therefore passing
  * invalid pointers may have unexpected results, including but not limited to SEGFAULTing the VM.
  *
  * See SerialConnection for a higher-level, more secured wrapper
  * of serial communication.
  *
  * @param serialAddr address of natively allocated serial configuration structure
  */
@nativeLoader("flow4")
private[flow] class UnsafeSerial(final val serialAddr: Long) {

  final val ParityNone: Int = 0
  final val ParityOdd: Int = 1
  final val ParityEven: Int = 2

  /**
    * Reads from a previously opened serial port into a direct ByteBuffer. Note that data is only
    * read into the buffer's allocated memory, its position or limit are not changed.
    *
    * The read is blocking, however it may be interrupted by calling cancelRead() on the given
    * serial port.
    *
    * @param buffer direct ByteBuffer to read into
    * @return number of bytes actually read
    * @throws IllegalArgumentException if the ByteBuffer is not direct
    * @throws PortInterruptedException if the call to this function was interrupted
    * @throws IOException on IO error
    */
  @native def read(buffer: ByteBuffer): Int

  /**
    * Cancels a read (any caller to read or readDirect will return with a
    * PortInterruptedException). This function may be called from any thread.
    *
    * @param serial address of natively allocated serial configuration structure
    * @throws IOException on IO error
    */
  @native def cancelRead(): Unit

  /**
    * Writes data from a direct ByteBuffer to a previously opened serial port. Note that data is
    * only taken from the buffer's allocated memory, its position or limit are not changed.
    *
    * The write is non-blocking, this function returns as soon as the data is copied into the kernel's
    * transmission buffer.
    *
    * @param serial address of natively allocated serial configuration structure
    * @param buffer direct ByteBuffer from which data is taken
    * @param length actual amount of data that should be taken from the buffer (this is needed since the native
    * backend does not provide a way to query the buffer's current limit)
    * @return number of bytes actually written
    * @throws IllegalArgumentException if the ByteBuffer is not direct
    * @throws IOException on IO error
    */
  @native def write(buffer: ByteBuffer, length: Int): Int

  /**
    * Closes an previously open serial port. Natively allocated resources are freed and the serial
    * pointer becomes invalid, therefore this function should only be called ONCE per open serial
    * port.
    *
    * A port should not be closed while it is used (by a read or write) as this
    * results in undefined behaviour.
    *
    * @param serial address of natively allocated serial configuration structure
    * @throws IOException on IO error
    */
  @native def close(): Unit

}

private[flow] object UnsafeSerial {

  /**
    * Opens a serial port.
    *
    * @param port name of serial port to open
    * @param characterSize size of a character of the data sent through the serial port
    * @param twoStopBits set to use two stop bits instead of one
    * @param parity type of parity to use with serial port
    * @return address of natively allocated serial configuration structure
    * @throws NoSuchPortException if the given port does not exist
    * @throws AccessDeniedException if permissions of the current user are not sufficient to open port
    * @throws PortInUseException if port is already in use
    * @throws InvalidSettingsException if any of the specified settings are invalid
    * @throws IOException on IO error
    */
  @native def open(port: String, baud: Int, characterSize: Int, twoStopBits: Boolean, parity: Int): Long

   /**
    * Sets native debugging mode. If debugging is enabled, detailed error messages
    * are printed (to stderr) from native method calls.
    *
    * @param value set to enable debugging
    */
  @native def debug(value: Boolean): Unit

}
