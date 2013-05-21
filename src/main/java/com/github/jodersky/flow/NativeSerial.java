package com.github.jodersky.flow;

public class NativeSerial {
	
	static {
		System.loadLibrary("flow");
	}
	
	final static int E_PERMISSION = -1;
	final static int E_OPEN = -2;
	final static int E_BUSY = -3;
	final static int E_BAUD = -4;
	final static int E_PIPE = -5;
	final static int E_MALLOC = -6;
	final static int E_POINTER = -7;
	final static int E_POLL = -8;
	final static int E_IO = -9;
	final static int E_CLOSE = -10;
	

	/* return values:
	 * 0 ok
	 * E_PERMISSION don't have permission to open
	 * E_OPEN can't get file descriptor
	 * E_BUSY device busy
	 * E_BAUD invalid baudrate
	 * E_PIPE can't open pipe for graceful closing
	 * E_MALLOC malloc error */
	native static int open(String device, int baud, long[] serial);
	
	/* return
	 * >0 number of bytes read
	 * E_POINTER invalid serial pointer
	 * E_POLL poll error
	 * E_IO read error
	 * E_CLOSE close request */
	native static int read(long serial, byte[] buffer);
	
	/*return
	 * >0 number of bytes written
	 * E_POINTER invalid serial config (null pointer)
	 * E_IO write error */
	native static int write(long serial, byte[] buffer);
	
	native static void close(long serial);
	
	native static void debug(boolean value);

}
