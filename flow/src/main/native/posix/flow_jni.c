#include "flow.h"
#include "com_github_jodersky_flow_internal_NativeSerial.h"

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
