package com.github.jodersky.flow

import akka.actor.{ ExtendedActorSystem, Props }
import akka.io.IO

/** Provides the serial IO manager. */
class SerialExt(system: ExtendedActorSystem) extends IO.Extension {
  lazy val manager = system.systemActorOf(Props(classOf[SerialManager]), name = "IO-SERIAL")
}
