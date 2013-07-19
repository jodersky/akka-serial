#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <fcntl.h>
#include "com_github_jodersky_flow_internal_NativeSerial.h"
#include "flow.h"

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
  const char* port_name,
  int baud,
  int char_size,
  bool two_stop_bits,
  int parity,
  struct serial_config** serial) {
  
  int fd = open(port_name, O_RDWR | O_NOCTTY | O_NONBLOCK);
  
  if (fd < 0) {
    int en = errno;
    DEBUG(perror("obtain file descriptor"););
    if (en == EACCES) return E_ACCESS_DENIED;
    if (en == ENOENT) return E_NO_PORT;
    return E_IO;
  }
  
  if (flock(fd, LOCK_EX | LOCK_NB) < 0) {
    DEBUG(perror("acquire lock on port"););
    close(fd);
    return E_BUSY;
  }
  
  /* configure new port settings */
  struct termios newtio;
  
  /* following calls correspond to makeraw() */
  newtio.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
  newtio.c_oflag &= ~OPOST;
  newtio.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
  newtio.c_cflag &= ~(CSIZE | PARENB);
  
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
      return E_INVALID_SETTINGS;
  }
  
  if (cfsetspeed(&newtio, bd) < 0) {
    DEBUG(perror("set baud rate"););
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
      return E_INVALID_SETTINGS;
  }
  
  if (tcflush(fd, TCIOFLUSH) < 0) {
    DEBUG(perror("flush serial settings"););
    close(fd);
    return E_IO;
  }
  
  if (tcsetattr(fd, TCSANOW, &newtio) < 0) {
    DEBUG(perror("apply serial settings"););
    close(fd);
    return E_IO;
  }
  
  int pipe_fd[2];
  if (pipe(pipe_fd) < 0) {
    DEBUG(perror("open pipe"););
    close(fd);
    return E_IO;
  }
  
  if (fcntl(pipe_fd[0], F_SETFL, O_NONBLOCK) < 0 || fcntl(pipe_fd[1], F_SETFL, O_NONBLOCK) < 0) {
    DEBUG(perror("make pipe non-blocking"););
    close(fd);
    return E_IO;
  }
  
  struct serial_config* s = malloc(sizeof(s));
  if (s == NULL) {
    DEBUG(perror("allocate memory for serial configuration"););
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

int serial_close(struct serial_config* serial) {
  if (close(serial->pipe_write_fd) < 0) {
    DEBUG(perror("close write end of pipe"););
    return E_IO;
  }
  if (close(serial->pipe_read_fd) < 0) {
    DEBUG(perror("close read end of pipe"););
    return E_IO;
  }
  
  if (flock(serial->port_fd, LOCK_UN) < 0){
    DEBUG(perror("release lock on port"););
    return E_IO;
  }
  if (close(serial->port_fd) < 0) {
    DEBUG(perror("close port"););
    return E_IO;
  }
  
  free(serial);
  return 0;
}

int serial_read(struct serial_config* serial, unsigned char* buffer, size_t size) {
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
    DEBUG(perror("select"););
    return E_IO;
  }
  
  if (FD_ISSET(port, &rfds)) {
    int r = read(port, buffer, size);
    
    //treat 0 bytes read as an error to avoid problems on disconnect
    //anyway, after a poll there should be more than 0 bytes available to read
    if (r <= 0) {
      DEBUG(perror("read"););
      return E_IO;
    }
    return r;
  } else if (FD_ISSET(pipe, &rfds)) {
    return E_INTERRUPT;
  } else {
    fputs("select: unknown read sets", stderr);
    return E_IO;
  }
}

int serial_write(struct serial_config* serial, unsigned char* data, size_t size) {
  int r = write(serial->port_fd, data, size);
  if (r < 0) {
    DEBUG(perror("write"););
    return E_IO;
  }
  return r;
}

int serial_interrupt(struct serial_config* serial) {
  int data = 0xffffffff;
  
  //write to pipe to wake up any blocked read thread (self-pipe trick)
  if (write(serial->pipe_write_fd, &data, 1) < 0) {
    DEBUG(perror("write to pipe for interrupt"););
    return E_IO;
  }
  
  return 0;
}


// JNI bindings
// ============

inline struct serial_config* j2s(jlong pointer) {
  return (struct serial_config*) pointer;
}

inline jlong s2j(struct serial_config* pointer) {
  return (jlong) pointer;
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_open
  (JNIEnv *env, jclass clazz, jstring port_name, jint baud, jint char_size, jboolean two_stop_bits, jint parity, jlongArray jserialp)
{ 
  const char *dev = (*env)->GetStringUTFChars(env, port_name, 0);
  struct serial_config* serial;
  int r = serial_open(dev, baud, char_size, two_stop_bits, parity, &serial);
  (*env)->ReleaseStringUTFChars(env, port_name, dev);
  
  long serialp = s2j(serial);
  (*env)->SetLongArrayRegion(env, jserialp, 0, 1, &serialp);
  
  return r;
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_close
  (JNIEnv * env, jclass clazz, jlong serial)
{
  serial_close(j2s(serial));
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_read
  (JNIEnv * env, jclass clazz, jlong serial, jbyteArray jbuffer)
{
  
  jsize size = (*env)->GetArrayLength(env, jbuffer);
  
  unsigned char buffer[size];
  int n = serial_read(j2s(serial), buffer, size);
  if (n < 0) {
    return n;
  }
  
  (*env)->SetByteArrayRegion(env, jbuffer, 0, n, (signed char *) buffer);
  return n;
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_write
  (JNIEnv * env, jclass clazz, jlong serial, jbyteArray jbuffer)
{
  unsigned char * buffer = (*env)->GetByteArrayElements(env, jbuffer, NULL);
  int size = (*env)->GetArrayLength(env, jbuffer);
  int r = serial_write(j2s(serial), buffer, size);
  
  (*env)->ReleaseByteArrayElements(env, jbuffer, buffer, JNI_ABORT);
  
  return r;
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_interrupt
  (JNIEnv * env, jclass clazz, jlong serial)
{
  return serial_interrupt(j2s(serial));
}

JNIEXPORT void JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_debug
  (JNIEnv *env, jclass clazz, jboolean value)
{
  serial_debug((bool) value);
}
