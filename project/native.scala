package native

import java.io.File
import sbt._
import sbt.Keys._
import scala.util.Try

/** A platform is a the representation of an os-architecture combination */
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
    val lines = Try { Process("uname -sm").lines.head }.toOption
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

object NativeKeys {

  val Native = config("native")

  val platform = settingKey[Platform]("Platform of the system this build is being run on.")

  //fat jar settings
  val libraryPrefix = settingKey[String]("A string to be prepended to native products when packaged.")
  val libraryManifest = settingKey[String]("Name of a file that will contain a list of all native products.")
  val libraryResourceDirectory = settingKey[File](
    "Directory that contains native products when they treated as resources."
  )

}

//windows, as usual, needs special treatment
object CygwinUtil {

  def onCygwin: Boolean = {
    val uname = Process("uname").lines.headOption
    uname map {
      _.toLowerCase.startsWith("cygwin")
    } getOrElse {
      false
    }
  }

  def toUnixPath(path: String): String = if (onCygwin) {
    Process(s"cygpath ${path}").lines.head
  } else {
    path
  }
  
}

/** Provides implementations of wrapper tasks suitable for projects using Autotools */
object Autotools {
  import NativeKeys._
  import sbt.Def.Initialize

  private val clean: Initialize[Task[Unit]] = Def.task {
    val log = streams.value.log
    val src = (sourceDirectory in Native).value

    Process("make distclean", src) #|| Process("make clean", src) ! log
  }

  private val lib: Initialize[Task[File]] = Def.task {
    val log = streams.value.log
    val src = (sourceDirectory in Native).value
    val out = (target in Native).value
    val outPath = CygwinUtil.toUnixPath(out.getAbsolutePath)

    val configure = if ((src / "config.status").exists) {
      Process("sh ./config.status", src)
    } else {
      Process(
        //Disable producing versioned library files, not needed for fat jars.
        s"sh ./configure --prefix=$outPath --libdir=$outPath --disable-versioned-lib",
        src
      )
    }

    val make = Process("make", src)

    val makeInstall = Process("make install", src)

    val ev = configure #&& make #&& makeInstall ! log
    if (ev != 0)
      throw new RuntimeException(s"Building native library failed. Exit code: ${ev}")

    val products: List[File] = (out ** ("*" -- "*.la")).get.filter(_.isFile).toList

    //only one produced library is expected
    products match {
      case Nil =>
        sys.error("No files were created during compilation, " +
          "something went wrong with the autotools configuration.")
      case head :: Nil =>
        head
      case head :: tail =>
        log.warn("More than one file was created during compilation, " +
          s"only the first one (${head.getAbsolutePath}) will be used.")
        head
    }
  }

  val settings: Seq[Setting[_]] = Seq(
    Keys.clean in Native := Autotools.clean.value,
    Keys.compile in Native := {
      lib.value
      sbt.inc.Analysis.Empty
    },
    Keys.packageBin in Native := {
      lib.value
    }
  )
}

object NativeDefaults {
  import NativeKeys._

  /** Copy native product to resource directory and create manifest */
  private val libraryResources = Def.task {
    val out = (libraryResourceDirectory in Compile).value

    val product = (packageBin in Native).value

    val productResource = out / product.name
    val manifestResource = out / (libraryManifest in Compile).value

    IO.copyFile(product, productResource)
    IO.write(manifestResource, productResource.name)

    Seq(productResource, manifestResource)
  }

  private val fatJarSettings = Seq(
    libraryPrefix in Compile := "",
    libraryManifest in Compile := "library",
    libraryResourceDirectory in Compile := (resourceManaged in Compile).value /
      (libraryPrefix in Compile).value / (platform in Native).value.id,
    unmanagedResourceDirectories in Compile += (baseDirectory).value / "lib_native",
    resourceGenerators in Compile += libraryResources.taskValue
  )

  val settings: Seq[Setting[_]] = Seq(
    platform in Native := Platform.uname.getOrElse {
      System.err.println("Warning: Cannot determine platform! It will be set to 'unknown'.")
      Platform("unknown", "unknown")
    },
    target in Native := target.value / "native" / (platform in Native).value.id
  ) ++ fatJarSettings ++ Autotools.settings

}

