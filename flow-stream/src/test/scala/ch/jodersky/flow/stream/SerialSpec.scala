package ch.jodersky.flow
package stream

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest._

class SerialSpec extends WordSpec with BeforeAndAfterAll with PseudoTerminal {

  implicit val system = ActorSystem("flow-test")
  implicit val materializer = ActorMaterializer()

  override def afterAll {
    system.terminate()
  }

  "Serial stream" should {
    val data = ByteString(("hello world").getBytes("utf-8"))

    "receive the same data it sends in an echo test" in {
      withEcho { case (port, settings) =>
        val graph = Source.single(data)
          .via(Serial().open(port, settings)) // send to echo pty
          .scan(ByteString.empty)(_ ++ _) // received elements could potentially be split by OS
          .dropWhile(_ != data)
          .toMat(Sink.head)(Keep.right)

        Await.result(graph.run(), 2.seconds)
      }
    }

    "fail if the underlying pty fails" in {
      val result = withEcho { case (port, settings) =>
        Source.single(data)
          .via(Serial().open(port, settings))
          .toMat(Sink.last)(Keep.right)
          .run()}

      intercept[StreamSerialException] {
        Await.result(result, 10.seconds)
      }
    }

  }

}
