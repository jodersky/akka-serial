#include "flow.h"
#include "com_github_jodersky_flow_internal_NativeSerial.h"


static inline void throwException(JNIEnv* env, const char* const exception, const char * const message) {
  (*env)->ThrowNew(env, (*env)->FindClass(env, exception), message); 
}

static inline void check(JNIEnv* env, int id) {
  switch (id) {
    case E_IO: throwException(env, "java/io/IOException", ""); break;
    case E_BUSY: throwException(env, "com/github/jodersky/flow/PortInUseException", ""); break;
    case E_ACCESS_DENIED: throwException(env, "com/github/jodersky/flow/AccessDeniedException", ""); break;
    case E_INVALID_SETTINGS: throwException(env, "com/github/jodersky/flow/InvalidSettingsException", ""); break;
    case E_INTERRUPT: throwException(env, "com/github/jodersky/flow/PortInterruptedException", ""); break;
    case E_NO_PORT: throwException(env, "com/github/jodersky/flow/NoSuchPortException", ""); break;
    default: return;
  }
}
 

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    open
 * Signature: (Ljava/lang/String;IIZI)J
 */
JNIEXPORT jlong JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_open
  (JNIEnv *env, jclass clazz, jstring port_name, jint baud, jint char_size, jboolean two_stop_bits, jint parity) { 

    const char *dev = (*env)->GetStringUTFChars(env, port_name, 0);
    struct serial_config* config;
    int r = serial_open(dev, baud, char_size, two_stop_bits, parity, &config);
    (*env)->ReleaseStringUTFChars(env, port_name, dev);

  
    if (r < 0) {
      check(env, r);
      return 0;
    }
  
    long jpointer = (long) config;
    return jpointer;

}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    readDirect
 * Signature: (JLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_readDirect
  (JNIEnv *env, jclass clazz, jlong config, jobject buffer) {

    char* local_buffer = (char*) (*env)->GetDirectBufferAddress(env, buffer);
    if (local_buffer == NULL) {
        throwException(env, "java/lang/IllegalArgumentException", "ByteBuffer not direct");
        return -1;
    }
    jlong size = (*env)->GetDirectBufferCapacity(env, buffer);

    int r = serial_read((struct serial_config*) config, local_buffer, (size_t) size);
    if (r < 0) {
      check(env, r);
      return -1;
    }
    return r;

}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    read
 * Signature: (J[B)I
 */
JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_read
  (JNIEnv *env, jclass clazz, jlong config, jbyteArray buffer) {

    jsize size = (*env)->GetArrayLength(env, buffer);
    char local_buffer[size];
    int r = serial_read((struct serial_config*) config, local_buffer, size);
    if (r < 0) {
      check(env, r);
      return -1;
    }

    (*env)->SetByteArrayRegion(env, buffer, 0, r, (signed char *) local_buffer);
    return r;

}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    cancelRead
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_cancelRead
  (JNIEnv *env, jclass clazz, jlong config) {

  int r = serial_cancel_read((struct serial_config*) config);
  if (r < 0) {
    check(env, r);
  }
    
}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    writeDirect
 * Signature: (JLjava/nio/ByteBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_writeDirect
  (JNIEnv *env, jclass clazz, jlong config, jobject buffer, jint size) {

    char* local_buffer = (char *) (*env)->GetDirectBufferAddress(env, buffer);
    if (local_buffer == NULL) {
        throwException(env, "java/lang/IllegalArgumentException", "ByteBuffer not direct");
        return -1;
    }

    int r = serial_write((struct serial_config*) config, local_buffer, (size_t) size);
    if (r < 0) {
      check(env, r);
      return -1;
    }
    return r;
    
}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    write
 * Signature: (J[BI)I
 */
JNIEXPORT jint JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_write
  (JNIEnv *env, jclass clazz, jlong config, jbyteArray buffer, jint size) {
    
    char* local_buffer = (char*) (*env)->GetByteArrayElements(env, buffer, NULL);
    int r = serial_write((struct serial_config*) config, local_buffer, size);
    (*env)->ReleaseByteArrayElements(env, buffer, (signed char*) local_buffer, JNI_ABORT);
    if (r < 0) {
      check(env, r);
      return -1;
    }
    return r;

}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_close
  (JNIEnv *env, jclass clazz, jlong config) {
    int r = serial_close((struct serial_config*) config);
    if (r < 0) {
      check(env, r);
    }
}

/*
 * Class:     com_github_jodersky_flow_internal_NativeSerial
 * Method:    debug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_github_jodersky_flow_internal_NativeSerial_debug
  (JNIEnv *env, jclass clazz, jboolean value) {
    serial_debug((bool) value);
}