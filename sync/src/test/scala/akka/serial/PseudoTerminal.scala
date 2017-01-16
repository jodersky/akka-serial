package akka.serial

import java.io.{File, IOException}
import java.nio.file.Files

import scala.concurrent.duration._
import scala.sys.process._
import scala.util.control.NonFatal

trait PseudoTerminal {

  final val SetupTimeout = 100.milliseconds

  def withEcho[A](action: (String, SerialSettings) => A): A = {
    val dir = Files.createTempDirectory("flow-pty").toFile
    val pty = new File(dir, "pty")

    val socat = try {
      val s = Seq(
        "socat",
        "-d -d",
        s"exec:cat,pty,raw,b115200,echo=0",
        s"pty,raw,b115200,echo=0,link=${pty.getAbsolutePath}"
      ).run(ProcessLogger(println(_)), false)
      Thread.sleep(SetupTimeout.toMillis) // allow ptys to set up
      s
    } catch {
      case NonFatal(ex) =>
        throw new IOException(
          "Error running echo service, make sure the program 'socat' is installed", ex)
    }

    try {
      val result = action(pty.getAbsolutePath, SerialSettings(baud = 115200))
      Thread.sleep(SetupTimeout.toMillis) // allow for async cleanup before destroying ptys
      result
    } finally {
      socat.destroy()
      dir.delete()
    }
  }

}
