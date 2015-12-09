#ifndef FLOW_H
#define FLOW_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include <stddef.h>

//general error codes that are returned by functions
#define E_IO -1 //IO error
#define E_ACCESS_DENIED -2 //access denied
#define E_BUSY -3 // port is busy
#define E_INVALID_SETTINGS -4 // some port settings are invalid
#define E_INTERRUPT -5 // not really an error, function call aborted because port is closed
#define E_NO_PORT -6 //requested port does not exist

#define PARITY_NONE 0
#define PARITY_ODD 1
#define PARITY_EVEN 2

/** Contains internal configuration of an open serial port. */
struct serial_config;

/**Opens a serial port and allocates memory for storing configuration. Note: if this function fails,
 * any internally allocated resources will be freed.
 * @param port_name name of port
 * @param baud baud rate
 * @param char_size character size of data transmitted through serial device
 * @param two_stop_bits set to use two stop bits instead of one
 * @param parity kind of parity checking to use
 * @param serial pointer to memory that will be allocated with a serial structure
 * @return 0 on success
 * @return E_NO_PORT if the given port does not exist
 * @return E_ACCESS_DENIED if permissions are not sufficient to open port
 * @return E_BUSY if port is already in use
 * @return E_INVALID_SETTINGS if any of the specified settings are invalid
 * @return E_IO on other error */
int serial_open(
    const char* port_name,
    int baud,
    int char_size,
    bool two_stop_bits,
    int parity,
    struct serial_config** const serial);

/**Closes a previously opened serial port and frees memory containing the configuration. Note: after a call to
 * this function, the 'serial' pointer will become invalid, make sure you only call it once. This function is NOT
 * thread safe, make sure no read or write is in prgress when this function is called (the reason is that per 
 * close manual page, close should not be called on a file descriptor that is in use by another thread). 
 * @param serial pointer to serial configuration that is to be closed (and freed)
 * @return 0 on success
 * @return E_IO on error */
int serial_close(struct serial_config* const serial);

/**Starts a read from a previously opened serial port. The read is blocking, however it may be
 * interrupted by calling 'serial_cancel_read' on the given serial port.
 * @param serial pointer to serial configuration from which to read
 * @param buffer buffer into which data is read
 * @param size maximum buffer size
 * @return n>0 the number of bytes read into buffer
 * @return E_INTERRUPT if the call to this function was interrupted
 * @return E_IO on IO error */
int serial_read(struct serial_config* const serial, char* const buffer, size_t size);

/**Cancels a blocked read call. This function is thread safe, i.e. it may be called from a thread even
 * while another thread is blocked in a read call.
 * @param serial_config the serial port to interrupt
 * @return 0 on success
 * @return E_IO on error */
int serial_cancel_read(struct serial_config* const serial);

/**Writes data to a previously opened serial port. Non bocking.
 * @param serial pointer to serial configuration to which to write
 * @param data data to write
 * @param size number of bytes to write from data
 * @return n>0 the number of bytes written
 * @return E_IO on IO error */
int serial_write(struct serial_config* const serial, char* const data, size_t size);

/**Sets debugging option. If debugging is enabled, detailed error message are printed from method calls. */
void serial_debug(bool value);

#ifdef __cplusplus
}
#endif


#endif /* FLOW_H */
