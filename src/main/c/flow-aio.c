#include <termios.h>
#include <stdio.h>
#include <stdbool.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/signal.h>
#include <sys/types.h>
#include <sys/file.h>
#include <errno.h>
#include <sys/epoll.h>
#include "com_github_jodersky_flow_NativeSerial.h"

#define BUFSIZE 128

/* return values:
 * >0 fd
 * -1 can't get file descriptor
 * -2 device busy
 * -3 invalid baudrate
 * */
int serial_open(const char* device, int baud) {
  
  int fd = open(device, O_RDWR | O_NOCTTY);
  
  if (fd < 0) {
    perror(device);
    return -1;
  }
  
  if (flock(fd, LOCK_EX | LOCK_NB) < 0) {
    perror(device);
    return -2;
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
    default: puts("invalid baudrate"); return -3; break;
  }
  
  struct aiocb my_aiocb;
  
  /* Zero out the aiocb structure (recommended) */
  bzero( (char *)&my_aiocb, sizeof(struct aiocb) );

  /* Allocate a data buffer for the aiocb request */
  my_aiocb.aio_buf = malloc(BUFSIZE+1);
  if (!my_aiocb.aio_buf) perror("malloc");

  /* Initialize the necessary fields in the aiocb */
  my_aiocb.aio_fildes = fd;
  my_aiocb.aio_nbytes = BUFSIZE;
  my_aiocb.aio_offset = 0;
  
  /* configure new port settings */
  struct termios newtio;  
  newtio.c_cflag &= ~PARENB;
  newtio.c_cflag &= ~CSTOPB;
  newtio.c_cflag &= ~CSIZE;
  newtio.c_cflag |= CS8;
  // no flow control
  newtio.c_cflag &= ~CRTSCTS;
  newtio.c_cflag &= ~HUPCL; // disable hang-up-on-close to avoid reset
  newtio.c_cflag |= CREAD | CLOCAL; // turn on READ & ignore ctrl lines
  newtio.c_iflag &= ~(IXON | IXOFF | IXANY); // turn off s/w flow ctrl
  newtio.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); // make raw
  newtio.c_oflag &= ~OPOST; // make raw

  // see: http://unixwiz.net/techtips/termios-vmin-vtime.html
  newtio.c_cc[VMIN] = 1;
  newtio.c_cc[VTIME] = 2*10/baud;
  cfsetspeed(&newtio, bd);
  
  /* load new settings to port */
  tcflush(fd, TCIFLUSH);
  tcsetattr(fd,TCSANOW,&newtio);
  
  int ret = aio_read( &my_aiocb );
  if (ret < 0) perror("aio_read");


  struct aiocb *cblist[1];
  /* Clear the list. */
  bzero( (char *)cblist, sizeof(cblist) );

  /* Load one or more references into the list */
  cblist[0] = &my_aiocb;
  aio_suspend(cblist, 1, NULL);
  //while ( aio_error( &my_aiocb ) == EINPROGRESS ) ;

  if ((ret = aio_return(&my_aiocb )) > 0) {
    /* got ret bytes on the read */
    puts("yeah, got bytes");
  } else {
    puts("hmmmmmm, couldn't read bytes");
  }
    
  return fd;
}

void serial_close(int fd) {
  if (flock(fd, LOCK_UN) < 0 ) {
    perror("");
  }
  if (close(fd) < 0 ) {
    perror("");
  }
}


// JNI bindings
// ============

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_NativeSerial_open
  (JNIEnv *env, jclass clazz, jstring device, jint baud, jobject reader)
{
  const char *dev = (*env)->GetStringUTFChars(env, device, 0);
  int r = serial_open(dev, baud);
  (*env)->ReleaseStringUTFChars(env, device, dev);
  return r;
}

/*
 * Class:     com_github_jodersky_flow_NativeSerial
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT void JNICALL Java_com_github_jodersky_flow_NativeSerial_close
  (JNIEnv * env, jclass clazz, jint fd)
{
  serial_close(fd);
}
