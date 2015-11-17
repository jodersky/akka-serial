package com.github.jodersky.flow
package internal

import java.io.{File, FileOutputStream, InputStream, OutputStream}
import scala.io.Source
import scala.sys.process.Process
import scala.util.Try

/** Handles loading of the current platform's native library for flow. */
object NativeLoader {

  /** A platform is the representation of an os-architecture combination */
  case class Platform(kernel: String, arch: String) {
    val id = kernel + "-" + arch
  }

  object Platform {

    /** Create a platform with spaces stripped and case normalized. */
    def normalize(kernel: String, arch: String) = Platform(
      kernel.toLowerCase.filter(!_.isWhitespace),
      arch
    )

    /** Run 'uname' to determine current platform. Returns None if uname does not exist. */
    lazy val uname: Option[Platform] = {
      val lines = Try { Process("uname -sm").lineStream.head }.toOption
      lines.map { line =>
        val parts = line.split(" ")
        if (parts.length != 2) {
          sys.error("Could not determine platform: 'uname -sm' returned unexpected string: " + line)
        } else {
          Platform.normalize(parts(0), parts(1))
        }
      }
    }

  }

  private final val BufferSize = 4096

  private final val LibraryManifest = "library"

  /** Extract a resource from this class loader to a temporary file. */
  private def extract(path: String): Option[File] = {
    var in: Option[InputStream] = None
    var out: Option[OutputStream] = None

    try {
      in = Option(NativeLoader.getClass.getResourceAsStream(path))
      if (in.isEmpty) return None

      val file = File.createTempFile(path, "")
      out = Some(new FileOutputStream(file))

      val buffer = new Array[Byte](BufferSize)
      var length = -1;
      do {
        length = in.get.read(buffer)
        if (length != -1) out.get.write(buffer, 0, length)
      } while (length != -1)

      Some(file)
    } finally {
      in.foreach(_.close)
      out.foreach(_.close)
    }
  }

  private def loadError(msg: String): Nothing = throw new UnsatisfiedLinkError(
    "Error during native library extraction " +
      "(this can happen if your platform is not supported by flow): " +
      msg
  )

  private def loadFromJar(libraryPrefix: String): Unit = {
    val platformDir: String = libraryPrefix + "/" + Platform.uname.map(_.id).getOrElse {
      loadError("Cannot determine current platform.")
    }

    val manifest: File = {
      val path = platformDir + "/" + LibraryManifest
      extract(path) getOrElse {
        loadError(s"Manifest file $path does not exist.")
      }
    }

    Source.fromFile(manifest, "utf-8").getLines foreach { libname =>
      val path = platformDir + "/" + libname
      val lib = extract(path) getOrElse loadError(
        s"Library $path not found."
      )
      System.load(lib.getAbsolutePath())
    }
  }

  /**
   * Load a native library from the available library path or fall back
   * to extracting and loading a native library from available resources.
   */
  def load(library: String, libraryPrefix: String): Unit = try {
    System.loadLibrary(library)
  } catch {
    case ex: UnsatisfiedLinkError => loadFromJar(libraryPrefix)
  }

}
