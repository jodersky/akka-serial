package com.github.jodersky.flow.internal

import java.io.File

import scalax.file.Path
import scalax.io.Resource

/** Handles loading of the current platform's native library for flow. */
object NativeLoader {

  private def os = System.getProperty("os.name").toLowerCase.filter(_ != ' ')

  private def arch = System.getProperty("os.arch").toLowerCase

  private def extractNative(nativeLibrary: String): Option[File] = {
    val fqlib = System.mapLibraryName(nativeLibrary) //fully qualified library name
  
    val in = NativeLoader.getClass().getResourceAsStream(s"/native/${os}-${arch}/${fqlib}")
    if (in == null) return None

    val temp = Path.createTempFile(nativeLibrary)
    Resource.fromInputStream(in).copyDataTo(temp)
    temp.fileOption
  }

  private def loadFromJar(nativeLibrary: String) = extractNative(nativeLibrary) match {
    case Some(file) => System.load(file.getAbsolutePath)
    case None => throw new UnsatisfiedLinkError("Cannot extract flow's native library, the native library may not exist for your specific architecture/OS combination.")
  }
  
  def load(nativeLibrary: String) = try {
    System.loadLibrary(nativeLibrary)
  } catch {
    case ex: UnsatisfiedLinkError => loadFromJar(nativeLibrary)
  }

}