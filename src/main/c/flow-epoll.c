#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <fcntl.h>
//#include <sys/signal.h>
//#include <sys/types.h>
//#include <sys/file.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include "com_github_jodersky_flow_NativeSerial.h"

//contains file descriptors used in managing a serial port
struct serial_config {
  
  int fd; //serial port
  int efd; //event
  int epfd; //file descriptor for epoll
  
};

/* return values:
 * >0 fd
 * -1 can't get file descriptor
 * -2 device busy
 * -3 invalid baudrate
 * -4 can't open pipe for graceful closing
 * -5 can't create epoll
 * -6 can't add event to epoll
 * */
int serial_open(const char* device, int baud, struct serial_config** serial) {
  
  int fd = open(device, O_RDWR | O_NOCTTY | O_NONBLOCK);
  
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
  
  int efd = eventfd(0, EFD_NONBLOCK);
  
  int epfd = epoll_create(1);
  if (epfd < 0) {
    perror(device);
    return -5;
  }
  
  struct epoll_event serial_event;
  serial_event.data.fd = fd;
  serial_event.events = EPOLLIN;
  
  struct epoll_event efd_event = {0};
  efd_event.data.fd = efd;
  efd_event.events = EPOLLIN | EPOLLET  | EPOLLONESHOT;
  
  
  if (epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &serial_event) < 0 ||  epoll_ctl(epfd, EPOLL_CTL_ADD, efd, &efd_event) < 0) {
    perror(device);
    return -6;
  }
  
  struct serial_config* s = malloc(sizeof(s));
  s->fd = fd;
  s->efd = efd;
  s->epfd = epfd;
  (*serial) = s;
  
  return 0;
}
/*
int serial_pipe() {
  int p = pipe();
  if (p < 0) {
    perror("create pipe");
  }
  return p;
}

int serial_add_epoll(int fd, int pipe) {
  int epfd = epoll_create(1);
  
  struct epoll_event event;
  event.data.fd = fd;
  event.events = EPOLLIN;
  
  if (epoll_ctl (epfd, EPOLL_CTL_ADD, fd, &event)) {
    perror (device);
  }
  
  return epfd;
}

*/
void serial_close(struct serial_config* serial) {
  
  if (serial == NULL) return;
  
  //write to pipe to wake up any blocked read thread (self-pipe trick)
  eventfd_write(serial->efd, 1);
  
  close(serial->efd);
  flock(serial->fd, LOCK_UN);
  close(serial->fd);
  close(serial->epfd);
  
  free(serial);
}

int serial_read(struct serial_config* serial) {
  struct epoll_event events[10];// = malloc (sizeof (struct epoll_event) * 10);
  int nr_events = epoll_wait(serial->epfd, events, 10, -1);
  if (nr_events < 0) {
    perror("read");
  }
  
  int i;
  for (i = 0; i < nr_events; i++) {
    //printf ("event=%d on fd=%d\n", events[i].events, events[i].data.fd);
    
    if (events[i].data.fd == serial->fd) puts("from serial");
    else if (events[i].data.fd == serial->efd) puts("from pipe");
    else puts("from ???");
  }
  /*
  int buffer[256];
  int x = read(fd, buffer, 255);
  if (x < 0) {
    perror("read");
  }*/
  return nr_events;
}


// JNI bindings
// ============

inline struct serial_config* j2s(jlong pointer) {
  return (struct serial_config*) pointer;
}

inline jlong s2j(struct serial_config* pointer) {
  return (jlong) pointer;
}

JNIEXPORT jlong JNICALL Java_com_github_jodersky_flow_NativeSerial_open
  (JNIEnv *env, jclass clazz, jstring device, jint baud)
{
  const char *dev = (*env)->GetStringUTFChars(env, device, 0);
  
  struct serial_config* serial;
  serial_open(dev, baud, &serial);
  
  (*env)->ReleaseStringUTFChars(env, device, dev);
  
  return s2j(serial);
}

JNIEXPORT void JNICALL Java_com_github_jodersky_flow_NativeSerial_close
  (JNIEnv * env, jclass clazz, jlong serial)
{
  serial_close(j2s(serial));
}

JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_NativeSerial_read
  (JNIEnv * env, jclass clazz, jlong serial)
{
  return serial_read(j2s(serial));
}
