package com.github.jodersky.flow

import akka.actor.ExtendedActorSystem
import akka.io.IO
import akka.actor.Props

class SerialExt(system: ExtendedActorSystem) extends IO.Extension {
  lazy val manager = system.actorOf(Props[SerialManager], name = "IO-SERIAL")
}