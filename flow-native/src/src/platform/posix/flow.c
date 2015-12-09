#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <fcntl.h>
#include <sys/file.h>
#include "flow.h"


#define DATA_CANCEL 0xffffffff

static bool debug = false;
#define DEBUG(f) if (debug) {f}

void serial_debug(bool value) {
    debug = value;
}

//contains file descriptors used in managing a serial port
struct serial_config {
    int port_fd; // file descriptor of serial port

    /* a pipe is used to abort a serial read by writing something into the
     * write end of the pipe */
    int pipe_read_fd; // file descriptor, read end of pipe
    int pipe_write_fd; // file descriptor, write end of pipe
};

int serial_open(
    const char* const port_name,
    int baud,
    int char_size,
    bool two_stop_bits,
    int parity,
    struct serial_config** serial) {

    int fd = open(port_name, O_RDWR | O_NOCTTY | O_NONBLOCK);
  
    if (fd < 0) {
        int en = errno;
        DEBUG(perror("error obtaining port file descriptor"););
        if (en == EACCES) return E_ACCESS_DENIED;
        if (en == ENOENT) return E_NO_PORT;
        return E_IO;
    }
  
    if (flock(fd, LOCK_EX | LOCK_NB) < 0) {
        DEBUG(perror("error acquiring lock on port"););
        close(fd);
        return E_BUSY;
    }
  
    /* configure new port settings */
    struct termios newtio;
  
    /* initialize serial interface */
    newtio.c_iflag = 0;
    newtio.c_oflag = 0;
    newtio.c_lflag = 0;
    newtio.c_cflag = CREAD;
  
    /* set speed */
    speed_t bd;
    switch (baud) {
        case 50: bd = B50; break;
        case 75: bd = B75; break;
        case 110: bd = B110; break;
        case 134: bd = B134; break;
        case 150: bd = B150; break;
        case 200: bd = B200; break;
        case 300: bd = B300; break;
        case 600: bd = B600; break;
        case 1200: bd = B1200; break;
        case 1800: bd = B1800; break;
        case 2400: bd = B2400; break;
        case 4800: bd = B4800; break;
        case 9600: bd = B9600; break;
        case 19200: bd = B19200; break;
        case 38400: bd = B38400; break;
        case 57600: bd = B57600; break;
        case 115200: bd = B115200; break;
        case 230400: bd = B230400; break;
        default:
            close(fd);
            DEBUG(fprintf(stderr, "invalid baud rate %d\n", baud););
            return E_INVALID_SETTINGS;
    }
  
    if (cfsetspeed(&newtio, bd) < 0) {
        DEBUG(perror("error setting baud rate"););
        close(fd);
        return E_IO;
    }

    /* set char size*/
    switch (char_size) {
        case 5: newtio.c_cflag |= CS5; break;
        case 6: newtio.c_cflag |= CS6; break;
        case 7: newtio.c_cflag |= CS7; break;
        case 8: newtio.c_cflag |= CS8; break;
        default:
            close(fd);
            DEBUG(fprintf(stderr, "invalid character size %d\n", char_size););
            return E_INVALID_SETTINGS;
    }
  
    /* use two stop bits */
    if (two_stop_bits){
        newtio.c_cflag |= CSTOPB;
    }
  
    /* set parity */
    switch (parity) {
        case PARITY_NONE: break;
        case PARITY_ODD: newtio.c_cflag |= (PARENB | PARODD); break;
        case PARITY_EVEN: newtio.c_cflag |= PARENB; break;
        default:
            close(fd);
            DEBUG(fprintf(stderr, "invalid parity %d\n", parity););
            return E_INVALID_SETTINGS;
    }
  
    if (tcflush(fd, TCIOFLUSH) < 0) {
        DEBUG(perror("error flushing serial settings"););
        close(fd);
        return E_IO;
    }
  
    if (tcsetattr(fd, TCSANOW, &newtio) < 0) {
        DEBUG(perror("error applying serial settings"););
        close(fd);
        return E_IO;
    }
  
    int pipe_fd[2];
    if (pipe(pipe_fd) < 0) {
        DEBUG(perror("error opening pipe"););
        close(fd);
        return E_IO;
    }
  
    if (fcntl(pipe_fd[0], F_SETFL, O_NONBLOCK) < 0 || fcntl(pipe_fd[1], F_SETFL, O_NONBLOCK) < 0) {
        DEBUG(perror("error setting pipe to non-blocking"););
        close(fd);
        close(pipe_fd[0]);
        close(pipe_fd[1]);
        return E_IO;
    }
  
    struct serial_config* s = malloc(sizeof(s));
    if (s == NULL) {
        DEBUG(perror("error allocating memory for serial configuration"););
        close(fd);
        close(pipe_fd[0]);
        close(pipe_fd[1]);
        return E_IO;
    }
  
    s->port_fd = fd;
    s->pipe_read_fd = pipe_fd[0];
    s->pipe_write_fd = pipe_fd[1];
    (*serial) = s;
  
    return 0;
}


int serial_close(struct serial_config* const serial) {
    if (close(serial->pipe_write_fd) < 0) {
        DEBUG(perror("error closing write end of pipe"););
        return E_IO;
    }
    if (close(serial->pipe_read_fd) < 0) {
        DEBUG(perror("error closing read end of pipe"););
        return E_IO;
    }
  
    if (flock(serial->port_fd, LOCK_UN) < 0){
        DEBUG(perror("error releasing lock on port"););
        return E_IO;
    }
    if (close(serial->port_fd) < 0) {
        DEBUG(perror("error closing port"););
        return E_IO;
    }
  
    free(serial);
    return 0;
}

int serial_read(struct serial_config* const serial, char* const buffer, size_t size) {
    int port = serial->port_fd;
    int pipe = serial->pipe_read_fd;

    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(port, &rfds);
    FD_SET(pipe, &rfds);
  
    int nfds = pipe + 1;
    if (pipe < port) nfds = port + 1;
  
    int n = select(nfds, &rfds, NULL, NULL, NULL);
    if (n < 0) {
        DEBUG(perror("error trying to call select on port and pipe"););
        return E_IO;
    }

    if (FD_ISSET(pipe, &rfds)) {
        return E_INTERRUPT;
    } else if (FD_ISSET(port, &rfds)) {
        int r = read(port, buffer, size);
    
        //treat 0 bytes read as an error to avoid problems on disconnect
        //anyway, after a poll there should be more than 0 bytes available to read
        if (r <= 0) {
            DEBUG(perror("error data not available after select"););
            return E_IO;
        }
        return r;
    } else {
        fprintf(stderr, "select returned unknown read sets\n");
        return E_IO;
    }
}

int serial_cancel_read(struct serial_config* const serial) {
    int data = DATA_CANCEL;
  
    //write to pipe to wake up any blocked read thread (self-pipe trick)
    if (write(serial->pipe_write_fd, &data, 1) < 0) {
        DEBUG(perror("error writing to pipe during read cancel"););
        return E_IO;
    }
  
    return 0;
}

int serial_write(struct serial_config* const serial, char* const data, size_t size) {
    int r = write(serial->port_fd, data, size);
    if (r < 0) {
        DEBUG(perror("error writing to port"););
        return E_IO;
    }
    return r;
}