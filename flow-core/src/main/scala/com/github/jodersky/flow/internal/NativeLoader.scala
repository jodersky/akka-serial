package com.github.jodersky.flow
package internal

import java.io.{ File, FileOutputStream, InputStream, OutputStream }

/** Handles loading of the current platform's native library for flow. */
object NativeLoader {

  private final val BufferSize = 4096

  private def os = System.getProperty("os.name").toLowerCase.replaceAll("\\s", "")

  private def arch = System.getProperty("os.arch").toLowerCase

  /** Extract a resource from this class loader to a temporary file. */
  private def extract(path: String, prefix: String): Option[File] = {
    var in: Option[InputStream] = None
    var out: Option[OutputStream] = None

    try {
      in = Option(NativeLoader.getClass.getResourceAsStream(path))
      if (in.isEmpty) return None

      val file = File.createTempFile(prefix, "")
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

  private def loadFromJar(library: String) = {
    val fqlib = System.mapLibraryName(library) //fully qualified library name
    val path = s"/native/${os}-${arch}/${fqlib}"
    extract(path, fqlib) match {
      case Some(file) => System.load(file.getAbsolutePath)
      case None => throw new UnsatisfiedLinkError("Cannot extract flow's native library, " +
        "the native library does not exist for your specific architecture/OS combination." +
        "Could not find " + path + ".")
    }
  }

  def load(library: String) = try {
    System.loadLibrary(library)
  } catch {
    case ex: UnsatisfiedLinkError => loadFromJar(library)
  }

}
