package com.github.jodersky.flow

/** The requested port could not be found. */
class NoSuchPortException(message: String) extends Exception(message)

/** The requested port is in use by someone else. */
class PortInUseException(message: String) extends Exception(message)

/** Permissions are not sufficient to open a serial port. */
class AccessDeniedException(message: String) extends Exception(message)

/** The settings specified are invalid. */
class InvalidSettingsException(message: String) extends Exception(message)

/** A blocking operation on a port was interrupted, most likely indicating that the port is closing. */
class PortInterruptedException(message: String) extends Exception(message)

/** The specified port has been closed. */
class PortClosedException(message: String) extends Exception(message)
