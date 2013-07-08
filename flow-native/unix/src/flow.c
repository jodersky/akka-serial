/*
 * Copyright (C) 2013 Jakob Odersky
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 * * Neither the name of the  nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
 
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <fcntl.h>
#include <poll.h>
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

int serial_open(const char* port_name, int baud, struct serial_config** serial) {
  
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
      return E_INVALID_BAUD;
      break;
  }

  /* configure new port settings */
  struct termios newtio;
  newtio.c_cflag &= ~(PARENB | CSTOPB | CSIZE | CRTSCTS); // 8N1
  newtio.c_cflag |= CS8 | CREAD | CLOCAL;
  newtio.c_iflag &= ~(IXON | IXOFF | IXANY); // turn off s/w flow ctrl
  newtio.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); // make raw
  newtio.c_oflag &= ~OPOST; // make raw

  //see: http://unixwiz.net/techtips/termios-vmin-vtime.html
  //newtio.c_cc[VMIN] = 1;
  //newtio.c_cc[VTIME] = 2*10/baud;
  
  if (cfsetspeed(&newtio, bd) < 0) {
    DEBUG(perror("set baud rate"););
    close(fd);
    return E_IO;
  }
  
  /* load new settings to port */
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
  
  struct pollfd polls[2];
  polls[0].fd = serial->port_fd; // serial poll
  polls[0].events = POLLIN;
  
  polls[1].fd = serial->pipe_read_fd; // pipe poll
  polls[1].events = POLLIN;
  
  int n = poll(polls,2,-1);
  if (n < 0) {
    DEBUG(perror("poll"););
    return E_IO;
  }
  
  if ((polls[0].revents & POLLIN) != 0) {
    int r = read(polls[0].fd, buffer, size);
    
    //treat 0 bytes read as an error to avoid problems on disconnect
    //anyway, after a poll there should be more than 0 bytes available to read
    if (r <= 0) {
      DEBUG(perror("read"););
      return E_IO;
    }
    return r;
  } else if ((polls[1].revents & POLLIN) != 0) {
    return E_INTERRUPT;
  } else {
    fputs("poll revents: unknown revents\n", stderr);
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
  (JNIEnv *env, jclass clazz, jstring port_name, jint baud, jlongArray jserialp)
{ 
  const char *dev = (*env)->GetStringUTFChars(env, port_name, 0);
  struct serial_config* serial;
  int r = serial_open(dev, baud, &serial);
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
