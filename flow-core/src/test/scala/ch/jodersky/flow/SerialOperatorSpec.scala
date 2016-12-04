package ch.jodersky.flow

import scala.concurrent.duration._

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import org.scalatest._

case class Ack(n: Int) extends Serial.Event

class SerialOperatorSpec
    extends TestKit(ActorSystem("serial-operator"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with SequentialNestedSuiteExecution
    with PseudoTerminal {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def withEchoOp[A](action: ActorRef => A): A = {
    withEcho { case (port, settings) =>
      val connection = SerialConnection.open(port, settings)
      val operator = system.actorOf(SerialOperator.apply(connection, 1024, testActor))
      action(operator)
    }
  }

  "Serial operator" should {

    "follow the correct protocol" in withEchoOp { op =>
      expectMsgType[Serial.Opened]

      val data = ByteString("hello world".getBytes("utf-8"))
      op ! Serial.Write(data)
      expectMsg(Serial.Received(data))

      op ! Serial.Close
      expectMsg(Serial.Closed)

    }

  }

}
