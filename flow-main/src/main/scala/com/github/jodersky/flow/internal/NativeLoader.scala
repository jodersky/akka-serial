package com.github.jodersky.flow.internal

import java.io.File
import java.io.FileOutputStream
import scalax.file.Path
import scalax.io.Resource
import scala.util.Try

/**Loads the current system's native library for flow. */
object NativeLoader {

  def extract(): Option[File] = {
    val os = System.getProperty("os.name").toLowerCase.filter(_ != ' ')
    val arch = System.getProperty("os.arch").toLowerCase
    val fqlib = System.mapLibraryName("flow") //fully qualified library name

    val in = NativeLoader.getClass().getResourceAsStream(s"/native/${os}/${arch}/${fqlib}")
    if (in == null) return None

    val temp = Path.createTempFile()
    Resource.fromInputStream(in).copyDataTo(temp)
    temp.fileOption
  }

  def loadFromJar() = extract() match {
    case Some(file) => System.load(file.getAbsolutePath)
    case None => throw new UnsatisfiedLinkError("cannot extract native library, the native library may not exist for your specific system/architecture combination")
  }

  def load = {
    try {
      System.loadLibrary("flow")
    } catch {
      case ex: UnsatisfiedLinkError => loadFromJar()
    }
  }

}