package akka.serial

import akka.actor.ActorSystem
import akka.io.IO
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

class SerialManagerSpec
    extends TestKit(ActorSystem("serial-manager"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with PseudoTerminal {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Serial manager" should {
    val manager = IO(Serial)

    "open an existing port" in {
      withEcho{ case (port, settings) =>
        manager ! Serial.Open(port, settings)
        expectMsgType[Serial.Opened]
      }
    }

    "fail opening a non-existing port" in {
      val cmd = Serial.Open("nonexistent", SerialSettings(115200))
      manager ! cmd
      assert(expectMsgType[Serial.CommandFailed].command == cmd)
    }

  }

}
