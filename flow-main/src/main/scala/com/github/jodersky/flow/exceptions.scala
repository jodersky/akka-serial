package com.github.jodersky.flow

import java.io.IOException

class NoSuchPortException(message: String) extends Exception(message)
class PortInUseException(message: String) extends Exception(message)
class AccessDeniedException(message: String) extends Exception(message)
class InvalidSettingsException(message: String) extends Exception(message)
class PortInterruptedException(message: String) extends Exception(message)
class PortClosedException(message: String) extends Exception(message)