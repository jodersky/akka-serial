package com.github.jodersky.flow

import akka.actor.ExtendedActorSystem
import akka.actor.Props
import akka.io.IO

class SerialExt(system: ExtendedActorSystem) extends IO.Extension {
  lazy val manager = system.actorOf(Props(classOf[SerialManager]), name = "IO-SERIAL")
}