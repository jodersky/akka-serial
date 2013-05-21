#include <termios.h>
#include <stdio.h>
#include <stdbool.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/signal.h>
#include <sys/types.h>
#include <sys/file.h>
#include "com_github_jodersky_flow_NativeSerial.h"


static JavaVM *jvm = NULL;
static jobject callback = NULL;

void signal_handler(int signum) {
  puts("got data");
  if(jvm == NULL)
        return ;
    if(callback == NULL)
        return ;

    JNIEnv *env = NULL;
    jint res;
    res = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
    if(res < 0)
    {
        fprintf(stderr, "Attach VM Thread failed\n");
        return ;
    }

    jclass cls = (*env)->GetObjectClass(env, callback);
    jmethodID mid = (*env)->GetMethodID(env, cls, "onRead", "()V");
    (*env)->CallVoidMethod(env, callback, mid);
    (*jvm)->DetachCurrentThread(jvm);
}

/* return values:
 * >0 fd
 * -1 can't get file descriptor
 * -2 device busy
 * -3 invalid baudrate
 * */
int serial_open(const char* device, int baud) {
  
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
  
  struct sigaction saio;
  saio.sa_handler = signal_handler;
  sigemptyset(&saio.sa_mask);
  saio.sa_flags = 0;
  saio.sa_restorer = NULL;
  sigaction(SIGIO,&saio,NULL);
  
  /* allow the process to receive SIGIO */
  fcntl(fd, F_SETOWN, getpid());
  
  /* send SIGIO whenever input or output becomes available */
  fcntl(fd, F_SETFL, FASYNC);
  
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
  (*env)->GetJavaVM(env, &jvm);
  /* upgrade callback to global ref */
  callback = (*env)->NewGlobalRef(env, reader);
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
