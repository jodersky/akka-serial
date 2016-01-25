package com.github.jodersky.flow
package samples.terminalstream

import akka.actor._
import akka.stream._
import akka.stream.stage._
import akka.stream.scaladsl._
import akka.util._
import akka.stream.io._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._
import akka.Done
import stream._

object Main {

  final val Delay = FiniteDuration(500, MILLISECONDS)

  implicit val system = ActorSystem("terminal-stream")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    val serial: Flow[ByteString, ByteString, Future[Serial.Connection]] =
      Serial().open("/dev/ttyACM0", SerialSettings(115200))

    val printer: Sink[ByteString, _] = Sink.foreach[ByteString]{data =>
      println("server says: " + data.decodeString("UTF-8"))
    }

    val ticker: Source[ByteString, _] = Source.tick(Delay, Delay, ()).scan(0){case (x, _) =>
      x + 1
    }.map{ x =>
      println(x)
      ByteString(x.toString)
    }

    val connection: Future[Serial.Connection] = ticker.viaMat(serial)(Keep.right).to(printer).run()

    connection map { conn =>
      println("Connected to " + conn.port)
      readLine("Press enter to exit")
    } recover { case err =>
      println("Cannot connect: " + err)
    } andThen { case _ =>
      system.terminate()
    }

  }

}
