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
#include <stdbool.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <fcntl.h>
#include <poll.h>
#include "com_github_jodersky_flow_low_NativeSerial.h"

#define E_PERMISSION -1
#define E_OPEN -2
#define E_BUSY -3
#define E_BAUD -4
#define E_PIPE -5
#define E_MALLOC -6
#define E_POINTER -7
#define E_POLL -8
#define E_IO -9
#define E_CLOSE -10


static bool debug = false;
#define DEBUG(f) if (debug) {f;}

//contains file descriptors used in managing a serial port
struct serial_config {
  
  int fd; //serial port
  int pipe_read; //event
  int pipe_write; //event
  
};

/* return values:
 * 0 ok
 * E_PERMISSION don't have permission to open
 * E_OPEN can't get file descriptor
 * E_BUSY device busy
 * E_BAUD invalid baudrate
 * E_PIPE can't open pipe for graceful closing
 * E_MALLOC malloc error
 */
int serial_open(const char* device, int baud, struct serial_config** serial) {
  
  int fd = open(device, O_RDWR | O_NOCTTY | O_NONBLOCK);
  
  if (fd < 0) {
    DEBUG(perror(device));
    if (errno == EACCES) return E_PERMISSION;
    else return E_OPEN;
  }
  
  if (flock(fd, LOCK_EX | LOCK_NB) < 0) {
    DEBUG(perror(device));
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
    default: return E_BAUD; break;
  }

  /* configure new port settings */
  struct termios newtio;
  newtio.c_cflag &= ~(PARENB | CSTOPB | CSIZE | CRTSCTS); // 8N1
  newtio.c_cflag |= CS8 | CREAD | CLOCAL;
  newtio.c_iflag &= ~(IXON | IXOFF | IXANY); // turn off s/w flow ctrl
  newtio.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); // make raw
  newtio.c_oflag &= ~OPOST; // make raw

  // see: http://unixwiz.net/techtips/termios-vmin-vtime.html
  //newtio.c_cc[VMIN] = 1;
  //newtio.c_cc[VTIME] = 2*10/baud;
  cfsetspeed(&newtio, bd);
  
  /* load new settings to port */
  tcflush(fd, TCIOFLUSH);
  tcsetattr(fd,TCSANOW,&newtio);
  
  int pipe_fd[2];
  if (pipe2(pipe_fd, O_NONBLOCK) < 0) {
    DEBUG(perror(device));
    return E_PIPE;
  }
  
  struct serial_config* s = malloc(sizeof(s));
  if (s == NULL) {
    return E_MALLOC;
  }
  
  s->fd = fd;
  s->pipe_read = pipe_fd[0];
  s->pipe_write = pipe_fd[1];
  (*serial) = s;
  
  return 0;
}

void serial_close(struct serial_config* serial) {
  
  if (serial == NULL) return;
  
  int data = 0xffffffff;
  
  //write to pipe to wake up any blocked read thread (self-pipe trick)
  if (write(serial->pipe_write, &data, 1) <= 0) {
    DEBUG(perror("error writing to pipe during close"))
  }
  
  close(serial->pipe_write);
  close(serial->pipe_read);
  
  flock(serial->fd, LOCK_UN);
  close(serial->fd);
  
  free(serial);
}

/* return
 * >0 number of bytes read
 * E_POINTER invalid serial pointer
 * E_POLL poll error
 * E_IO read error
 * E_CLOSE close request
 */
int serial_read(struct serial_config* serial, unsigned char * buffer, size_t size) {
  if (serial == NULL) {
    return E_POINTER;
  }
  
  
  struct pollfd polls[2];
  polls[0].fd = serial->fd; // serial poll
  polls[0].events = POLLIN;
  
  polls[1].fd = serial->pipe_read; // pipe poll
  polls[1].events = POLLIN;
  
  int n = poll(polls,2,-1);
  if (n < 0) {
    DEBUG(perror("read"));
    return E_IO;
  }
  
  if ((polls[0].revents & POLLIN) != 0) {
    int r = read(polls[0].fd, buffer, size);
    
    //treat 0 bytes read as an error to avoid problems on disconnect
    //anyway, after a poll there should be more than 0 bytes available to read
    if (r <= 0) { 
      if (r < 0) DEBUG(perror("read"));
      return E_IO;
    }
    return r;
  } else {
    return E_CLOSE;
  }
}

/*return
 * >0 number of bytes written
 * E_POINTER invalid serial config (null pointer)
 * E_IO write error
 */
int serial_write(struct serial_config* serial, unsigned char* data, size_t size) {
  if (serial == NULL) return E_POINTER;
  
  int r = write(serial->fd, data, size);
  if (r < 0) {
    DEBUG(perror("write"));
    return E_IO;
  }
  return r;
}



// JNI bindings
// ============

inline struct serial_config* j2s(jlong pointer) {
  return (struct serial_config*) pointer;
}

inline jlong s2j(struct serial_config* pointer) {
  return (jlong) pointer;
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_low_NativeSerial_open
  (JNIEnv *env, jclass clazz, jstring device, jint baud, jlongArray jserialp)
{ 
  const char *dev = (*env)->GetStringUTFChars(env, device, 0);
  struct serial_config* serial;
  int r = serial_open(dev, baud, &serial);
  (*env)->ReleaseStringUTFChars(env, device, dev);
  
  long serialp = s2j(serial);
  (*env)->SetLongArrayRegion(env, jserialp, 0, 1, &serialp);
  
  return r;
}

JNIEXPORT void JNICALL Java_com_github_jodersky_flow_low_NativeSerial_close
  (JNIEnv * env, jclass clazz, jlong serial)
{
  serial_close(j2s(serial));
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_low_NativeSerial_read
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

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_low_NativeSerial_write
  (JNIEnv * env, jclass clazz, jlong serial, jbyteArray jbuffer)
{
  unsigned char * buffer = (*env)->GetByteArrayElements(env, jbuffer, NULL);
  int size = (*env)->GetArrayLength(env, jbuffer);
  int r = serial_write(j2s(serial), buffer, size);
  
  (*env)->ReleaseByteArrayElements(env, jbuffer, buffer, JNI_ABORT);
  
  return r;
}

JNIEXPORT void JNICALL Java_com_github_jodersky_flow_low_NativeSerial_debug
  (JNIEnv *env, jclass clazz, jboolean value)
{
  debug = (bool) value;
}
