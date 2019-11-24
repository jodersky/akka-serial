package akka.serial

import java.io.{File, IOException}
import java.nio.file.Files

import scala.concurrent.duration._
import scala.sys.process._
import scala.util.control.NonFatal

trait PseudoTerminal {

  final val SetupTimeout = 100.milliseconds

  def withEcho[A](action: (String, SerialSettings) => A): A = {
    val dir = Files.createTempDirectory("akka-serial-pty").toFile
    val pty = new File(dir, "pty")

    val socat = Process(
      "socat",
      Seq(
        "-d", "-d",
        s"exec:cat,pty,raw,b115200,echo=0", 
        s"pty,raw,b115200,echo=0,link=${pty.getAbsoluteFile()}"
      ) 
    ).run(ProcessLogger(println(_)), false)

    Thread.sleep(SetupTimeout.toMillis) // allow ptys to set up
      
    if (!socat.isAlive()) {
      sys.error(s"socat exited too early with code ${socat.exitValue()}")
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
