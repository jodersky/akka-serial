import sbt._
import Keys._
import java.io.File
import scala.collection.mutable.HashSet

case class NativeBuild(
  name: String,
  cCompiler: String,
  cFlags: Seq[String],
  linker: String,
  linkerFlags: Seq[String],
  binary: String)

object NativeKeys {
  
  //build settings
  val nativeBuilds = taskKey[Seq[NativeBuild]]("All native build configurations, including cross-compilation.")
  val nativeVersion = settingKey[String]("Version of native binary")

  //compile settings
  val nativeIncludeDirectories = settingKey[Seq[File]]("Directories to include during build (gcc -I option)")
  
  //link settings
  val nativeLibraries = settingKey[Seq[String]]("Default names of libraries to use during linking.")
  val nativeLibraryDirectories = settingKey[Seq[File]]("Directories to search for libraries (gcc -L option)")
  
  //directories
  val nativeSource = settingKey[File]("Lowest level directory containing all native sources.")
  val nativeCSources = taskKey[Seq[File]]("All c source files.")
  val nativeTargetDirectory = settingKey[File]("Directory containing all compiled and linked files.")

  //tasks
  val nativeCompile = taskKey[Map[NativeBuild, Seq[File]]]("Compile all native build configurations.")
  val nativeLink = taskKey[Map[NativeBuild, File]]("Link all native build configurations.")
  
}

object NativeDefaults {
  import NativeKeys._

  private def generate(generators: SettingKey[Seq[Task[Seq[File]]]]) = generators { _.join.map(_.flatten) }

  private def compile(logger: Logger, compiler: String, flags: Seq[String], includeDirectories: Seq[File], src: File, out: File): File = {
    IO.createDirectory(out.getParentFile)
    val parts: Seq[String] =
      Seq(compiler) ++
      flags ++
      includeDirectories.map("-I" + _.getAbsolutePath) ++
      Seq("-o", out.getAbsolutePath) ++
      Seq("-c", src.getAbsolutePath)

    val cmd = parts.mkString(" ")
    logger.info(cmd)
    val ev = Process(cmd) ! logger
    if (ev != 0) throw new RuntimeException(s"Compilation of ${src.getAbsoluteFile()} failed.")
    out
  }

  private def link(logger: Logger, linker: String, flags: Seq[String], libraryDirectories: Seq[File], libraries: Seq[String], in: Seq[File], out: File): File = {
    val parts: Seq[String] =
      Seq(linker) ++
      flags ++
      Seq("-o", out.getAbsolutePath) ++
      in.map(_.getAbsolutePath) ++
      libraryDirectories.map("-L" + _.getAbsolutePath) ++
      libraries.map("-l" + _)

    val cmd = parts.mkString(" ")
    logger.info(cmd)
    val ev = Process(cmd) ! logger
    if (ev != 0) throw new RuntimeException(s"Linking of ${out.getAbsoluteFile()} failed.")
    out
  }


  def  nativeCompileImpl() = Def.task {
    val logger = streams.value.log
    val builds = nativeBuilds.value
    val outDir = nativeTargetDirectory.value
    val includeDirs = nativeIncludeDirectories.value
    val csrcs = nativeCSources.value

    val compilations = for (build <- builds) yield {
      logger.info("Compiling configuration " + build.name)
      val objects = for (src <- csrcs) yield {
        compile(logger, build.cCompiler, build.cFlags, includeDirs, src, outDir / build.name / "objects" / (src.base + ".o"))
      }
      build -> objects
    }
     compilations.toMap
  }

  lazy val nativeLinkImpl = Def.task {
    val logger = streams.value.log
    val builds = nativeBuilds.value
    val outDir = nativeTargetDirectory.value
    val libDirs = nativeLibraryDirectories.value
    val libs = nativeLibraries.value
    val compilations = nativeCompile.value
    val version = nativeVersion.value

    val linkages = for (build <- builds) yield {
      logger.info("Linking configuration " + build.name)
      val objects = compilations(build)
      val binary = link(logger, build.linker, build.linkerFlags, libDirs, libs, objects, outDir / build.name / build.binary)
      build -> binary
    }
    linkages.toMap
  }
  
  def localPlatform = try {
    Process("gcc -dumpmachine").lines.headOption
  } catch {
    case ex: Exception => None
  }

  
  val settings: Seq[Setting[_]] = Seq(
    //nativeBuilds :=

    nativeSource := (sourceDirectory in Compile).value / "native",
    includeFilter in nativeCSources := "*.c",
    nativeCSources := (nativeSource.value ** (includeFilter in nativeCSources).value).get,
    nativeTargetDirectory := target.value / "native",

    nativeIncludeDirectories := Seq(nativeSource.value, nativeSource.value / "include"),
    nativeLibraries := Seq(),
    nativeLibraryDirectories := Seq(),

    nativeCompile := nativeCompileImpl.value,
    nativeLink := nativeLinkImpl.value
  )
}
