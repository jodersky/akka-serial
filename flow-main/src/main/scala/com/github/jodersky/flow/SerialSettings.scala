package com.github.jodersky.flow

/**
 * Groups settings used in communication over a serial port.
 * @param baud baud rate to use with serial port
 * @param characterSize size of a character of the data sent through the serial port
 * @param twoStopBits set to use two stop bits instead of one
 * @param parity type of parity to use with serial port
 */
case class SerialSettings(baud: Int, characterSize: Int = 8, twoStopBits: Boolean = false, parity: Parity.Parity = Parity.None)
