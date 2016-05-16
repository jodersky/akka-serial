package ch.jodersky.flow
package samples.terminalstream

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString

import stream.Serial

object Main {

  final val Delay = FiniteDuration(500, MILLISECONDS)

  implicit val system = ActorSystem("terminal-stream")
  implicit val materializer = ActorMaterializer()

  def ask(label: String, default: String) = {
    print(label + " [" + default.toString + "]: ")
    val in = StdIn.readLine()
    println("")
    if (in.isEmpty) default else in
  }

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    val port = ask("Device", "/dev/ttyACM0")
    val baud = ask("Baud rate", "115200").toInt
    val cs = ask("Char size", "8").toInt
    val tsb = ask("Use two stop bits", "false").toBoolean
    val parity = Parity(ask("Parity (0=None, 1=Odd, 2=Even)", "0").toInt)
    val settings = SerialSettings(baud, cs, tsb, parity)

    val serial: Flow[ByteString, ByteString, Future[Serial.Connection]] =
      Serial().open(port, settings)

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
      StdIn.readLine("Press enter to exit")
    } recover { case err =>
      println("Cannot connect: " + err)
    } andThen { case _ =>
      system.terminate()
    }

  }

}
