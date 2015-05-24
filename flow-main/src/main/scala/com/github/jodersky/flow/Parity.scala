package com.github.jodersky.flow

/** Specifies available parities used in serial communication. */
object Parity extends Enumeration {
  type Parity = Value
  val None = Value(0)
  val Odd = Value(1)
  val Even = Value(2)
}
