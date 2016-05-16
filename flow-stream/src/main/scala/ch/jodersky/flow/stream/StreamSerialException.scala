package ch.jodersky.flow
package stream

/** Represents a generic exception occured during streaming of serial data. */
class StreamSerialException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)
