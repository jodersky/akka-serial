#include <stdint.h>

#include "flow.h"

#include "ch_jodersky_flow_UnsafeSerial.h"
#include "ch_jodersky_flow_UnsafeSerial__.h"

// suppress unused parameter warnings
#define UNUSED_ARG(x) (void)(x)

static inline void throwException(JNIEnv* env, const char* const exception, const char * const message)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, exception), message);
}

/** Check return code and throw exception in case it is non-zero. */
static void check(JNIEnv* env, int ret)
{
	switch (ret) {
	case -E_IO: throwException(env, "java/io/IOException", ""); break;
	case -E_BUSY: throwException(env, "ch/jodersky/flow/PortInUseException", ""); break;
	case -E_ACCESS_DENIED: throwException(env, "ch/jodersky/flow/AccessDeniedException", ""); break;
	case -E_INVALID_SETTINGS: throwException(env, "ch/jodersky/flow/InvalidSettingsException", ""); break;
	case -E_INTERRUPT: throwException(env, "ch/jodersky/flow/PortInterruptedException", ""); break;
	case -E_NO_PORT: throwException(env, "ch/jodersky/flow/NoSuchPortException", ""); break;
	default: return;
	}
}

/** Get pointer to serial config associated to an UnsafeSerial instance. */
static struct serial_config* get_config(JNIEnv* env, jobject unsafe_serial)
{
	jclass clazz = (*env)->FindClass(env, "ch/jodersky/flow/UnsafeSerial");
	jfieldID field = (*env)->GetFieldID(env, clazz, "serialAddr", "J");
	jlong addr = (*env)->GetLongField(env, unsafe_serial, field);
	return (struct serial_config*) (intptr_t) addr;
}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial__
 * Method:    open
 * Signature: (Ljava/lang/String;IIZI)J
 */
JNIEXPORT jlong JNICALL Java_ch_jodersky_flow_UnsafeSerial_00024_open
(JNIEnv *env, jobject instance, jstring port_name, jint baud, jint char_size, jboolean two_stop_bits, jint parity)
{
	UNUSED_ARG(instance);

	const char *dev = (*env)->GetStringUTFChars(env, port_name, 0);
	struct serial_config* config;
	int r = serial_open(dev, baud, char_size, two_stop_bits, parity, &config);
	(*env)->ReleaseStringUTFChars(env, port_name, dev);

	if (r < 0) {
		check(env, r);
		return -E_IO;
	}

	long jpointer = (long) config;
	return (jlong) jpointer;
}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial
 * Method:    read
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_ch_jodersky_flow_UnsafeSerial_read
(JNIEnv *env, jobject instance, jobject buffer)
{
	char* local_buffer = (char*) (*env)->GetDirectBufferAddress(env, buffer);
	if (local_buffer == NULL) {
		throwException(env, "java/lang/IllegalArgumentException", "buffer is not direct");
		return -E_IO;
	}
	size_t size = (size_t) (*env)->GetDirectBufferCapacity(env, buffer);
	struct serial_config* config = get_config(env, instance);

	int r = serial_read(config, local_buffer, size);
	if (r < 0) {
		check(env, r);
	}
	return r;

}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial
 * Method:    cancelRead
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ch_jodersky_flow_UnsafeSerial_cancelRead
(JNIEnv *env, jobject instance)
{
	int r = serial_cancel_read(get_config(env, instance));
	if (r < 0) {
		check(env, r);
	}
}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial
 * Method:    write
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_ch_jodersky_flow_UnsafeSerial_write
(JNIEnv *env, jobject instance, jobject buffer, jint size)
{

	char* local_buffer = (char *) (*env)->GetDirectBufferAddress(env, buffer);
	if (local_buffer == NULL) {
		throwException(env, "java/lang/IllegalArgumentException", "buffer is not direct");
		return -E_IO;
	}

	int r = serial_write(get_config(env, instance), local_buffer, (size_t) size);
	if (r < 0) {
		check(env, r);
		return -E_IO;
	}
	return r;
}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ch_jodersky_flow_UnsafeSerial_close
(JNIEnv *env, jobject instance)
{
	int r = serial_close(get_config(env, instance));
	if (r < 0) {
		check(env, r);
	}
}

/*
 * Class:     ch_jodersky_flow_UnsafeSerial__
 * Method:    debug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ch_jodersky_flow_UnsafeSerial_00024_debug
(JNIEnv *env, jobject instance, jboolean value)
{
	UNUSED_ARG(env);
	UNUSED_ARG(instance);

	serial_debug((bool) value);
}
