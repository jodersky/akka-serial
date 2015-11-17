package com.github.jodersky.flow.internal;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.jodersky.flow.AccessDeniedException;
import com.github.jodersky.flow.InvalidSettingsException;
import com.github.jodersky.flow.NoSuchPortException;
import com.github.jodersky.flow.PortInUseException;
import com.github.jodersky.flow.PortInterruptedException;

/**
 * Low-level wrapper on top of native serial backend. 
 * 
 * WARNING: Methods in this class allocate native structures and deal with pointers.
 * These pointers are handled as longs by java and are NOT checked for correctness,
 * therefore passing invalid pointers may have unexpected results, including but not
 * limited to crashing the VM.
 * 
 * See SerialConnection for a higher level, more secured wrapper
 * of serial communication.
 * 
 * @see com.github.jodersky.flow.internal.SerialConnection
 */
final class NativeSerial {
	
	static {
		NativeLoader.load("flow3", "/com/github/jodersky/flow/native");
	}
	
	final static int PARITY_NONE = 0;
	final static int PARITY_ODD = 1;
	final static int PARITY_EVEN = 2;
	
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
	native static long open(String port, int baud, int characterSize, boolean twoStopBits, int parity)
	throws NoSuchPortException, AccessDeniedException, PortInUseException, InvalidSettingsException, IOException;
	
	/**
	 * Reads from a previously opened serial port into a direct ByteBuffer. Note that data is only read into the
	 * buffer's allocated memory, its position or limit are not changed. 
	 *  
	 * The read is blocking, however it may be interrupted by calling cancelRead() on the given serial port.
	 * 
	 * @param serial address of natively allocated serial configuration structure
	 * @param buffer direct ByteBuffer to read into
	 * @return number of bytes actually read
	 * @throws IllegalArgumentException if the ByteBuffer is not direct
	 * @throws PortInterruptedException if the call to this function was interrupted
	 * @throws IOException on IO error
	 */
	native static int readDirect(long serial, ByteBuffer buffer)
	throws IllegalArgumentException, PortInterruptedException, IOException;

	/**
	 * Reads data from a previously opened serial port into an array.
	 * 
	 * The read is blocking, however it may be interrupted by calling cancelRead() on the given serial port.
	 * 
	 * @param serial address of natively allocated serial configuration structure
	 * @param buffer array to read data into
	 * @return number of bytes actually read
	 * @throws PortInterruptedException if the call to this function was interrupted
	 * @throws IOException on IO error
	 */
	native static int read(long serial, byte[] buffer)
	throws PortInterruptedException, IOException;
	
	/**
	 * Cancels a read (any caller to read or readDirect will return with a PortInterruptedException). This function may be called from any thread.
	 * 
	 * @param serial address of natively allocated serial configuration structure
	 * @throws IOException on IO error
	 */
	native static void cancelRead(long serial)
	throws IOException;

	/**
	 * Writes data from a direct ByteBuffer to a previously opened serial port. Note that data is only taken from
	 * the buffer's allocated memory, its position or limit are not changed.
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
	native static int writeDirect(long serial, ByteBuffer buffer, int length)
	throws IllegalArgumentException, IOException;
	
	/**
	 * Writes data from an array to a previously opened serial port.
	 *  
	 * The write is non-blocking, this function returns as soon as the data is copied into the kernel's
	 * transmission buffer.
	 * 
	 * @param serial address of natively allocated serial configuration structure
	 * @param buffer array from which data is taken
	 * @param length actual amount of data that should be taken from the buffer
	 * @return number of bytes actually written
	 * @throws IOException on IO error
	 */
	native static int write(long serial, byte[] buffer, int length)
	throws IOException;
	
	/**
	 * Closes an previously open serial port. Natively allocated resources are freed and the serial pointer becomes invalid,
	 * therefore this function should only be called ONCE per open serial port.
	 * 
	 * A port should not be closed while it is used (by a read or write) as this
	 * results in undefined behaviour.
	 * 
	 * @param serial address of natively allocated serial configuration structure
	 * @throws IOException on IO error
	 */
	native static void close(long serial)
	throws IOException;
	
	/**
	 * Sets native debugging mode. If debugging is enabled, detailed error messages
	 * are printed (to stderr) from native method calls.
	 * 
	 * @param value set to enable debugging
	 */
	native static void debug(boolean value);

}
