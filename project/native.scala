import sbt._
import Keys._
import java.io.File
import scala.collection.mutable.HashSet

object NativeKeys {
  
  val Native = config("native")
  
  //compilation
  val cCompiler = settingKey[String]("Default c compiler.")
  val cppCompiler = settingKey[String]("Default c++ comppiler.")
  val cFlags = settingKey[Seq[String]]("Default flags for c compiler.")
  val cppFlags = settingKey[Seq[String]]("Default flags for c++ compiler.")
  val flags = settingKey[Seq[String]]("Default flags prepended to c and c++ flags.")
  val nativeCompile = taskKey[Seq[File]]("Compile native sources.")
    
  val linker = settingKey[String]("Linker used in project.")
  val linkFlags = settingKey[Seq[String]]("Default options for linker.")
  val libraryNames = settingKey[Seq[String]]("Default names of libraries to use during linking.")
  val binaryName = settingKey[String]("Final binary product.")
  val link = taskKey[File]("Link compiled objects to a final product.")

  //directories
  val nativeSource = settingKey[File]("Lowest level directory containing all native sources.")
  val cSources = taskKey[Seq[File]]("All c source files, managed and unmanaged.")
  val cppSources = taskKey[Seq[File]]("All c++ source files, managed and unmanaged.")
  val includeDirectories = settingKey[Seq[File]]("Directories to include during build (gcc -I option)")
  val libraryDirectories = settingKey[Seq[File]]("Directories to search for libraries (gcc -L option)")
  val objectDirectory = settingKey[File]("Directory containing all compiled objects.")
  //val objects = taskKey[Seq[File]]("Object files generated from source files. Note: there should be a one-to-one mapping between source and object files.")
  
}

object NativeDefaults {
  import NativeKeys._

  private def generate(generators: SettingKey[Seq[Task[Seq[File]]]]) = generators { _.join.map(_.flatten) }

  def compileImpl = Def.task {
    implicit val log = streams.value.log

    def compile(compiler: String, flags: Seq[String], src: File) = {
      val obj = objectDirectory.value / (src.base + ".o")
      IO.createDirectory(obj.getParentFile())
      val parts: Seq[String] = Seq(compiler) ++
        flags ++
        includeDirectories.value.map("-I" + _.getAbsolutePath) ++
        Seq("-o", obj.getAbsolutePath()) ++
        Seq("-c", src.getAbsolutePath())

      val cmd = parts.mkString(" ")
      log.info(cmd)
      val ev = Process(cmd) ! log
      if (ev != 0) throw new RuntimeException(s"compilation of ${src.getAbsoluteFile()} failed")
      obj
    }

    cSources.value.map { src =>
      compile(cCompiler.value, cFlags.value, src)
    } ++
      cppSources.value.map { src =>
        compile(cppCompiler.value, cppFlags.value, src)
      }
  }

  def linkImpl = Def.task {
    implicit val log = streams.value.log

    val out = (target.value / binaryName.value)
    val parts: Seq[String] = Seq(linker.value) ++
      linkFlags.value ++
      Seq("-o", out.getAbsolutePath) ++
      nativeCompile.value.map(_.getAbsolutePath) ++
      libraryDirectories.value.map("-L" + _.getAbsolutePath) ++
      libraryNames.value.map("-l" + _)

    val cmd = parts.mkString(" ")
    log.info(cmd)
    val ev = Process(cmd) ! log
    if (ev != 0) throw new RuntimeException(s"linking of ${out.getAbsoluteFile()} failed")
    out
  }

  val compileSettings: Seq[Setting[_]] = inConfig(Native)(Seq(
    cCompiler := "gcc",
    cppCompiler := "g++",
    cFlags := flags.value,
    cppFlags := flags.value,
    flags := Seq("-fPIC", "-O2"),
    nativeCompile := compileImpl.value,
    sourceGenerators := Seq()))

  val linkSettings: Seq[Setting[_]] = inConfig(Native)(Seq(
    linker := "gcc",
    linkFlags := Seq(),
    libraryNames := Seq(),
    binaryName := binaryName.value,
    link := linkImpl.value))

  val fileSettings: Seq[Setting[_]] = inConfig(Native)(Seq(
    target := (target in Compile).value / "native",
    nativeSource := (sourceDirectory in Compile).value / "native",
    sourceManaged := target.value / "src_managed",
    unmanagedSources := (nativeSource.value ** (includeFilter in unmanagedSources).value).get,
    managedSources := (generate(sourceGenerators).value ** (includeFilter in managedSources).value).get,
    sources := unmanagedSources.value ++ managedSources.value,
    cSources := sources.value.filter(src => (includeFilter in cSources).value accept src),
    cppSources := sources.value.filter(src => (includeFilter in cppSources).value accept src),
    includeFilter in (unmanagedSources) := ("*.c" || "*.cpp" || "*.cxx" || "*.cc"),
    includeFilter in (managedSources) := (includeFilter in unmanagedSources).value,
    includeFilter in cSources := "*.c",
    includeFilter in cppSources := "*.cpp" || "*.cc" || "*.cxx",
    objectDirectory := target.value / "objects",
    includeDirectories := Seq(nativeSource.value, sourceManaged.value),
    libraryDirectories := Seq(),
    binaryName := (name in Compile).value))

  val defaultSettings: Seq[Setting[_]] = inConfig(Native)(
    compileSettings ++
      linkSettings ++
      fileSettings)
      
  implicit class RichNativeProject(project: Project) {
    def dependsOnNative(other: Project): Project = {
      val newSettings: Seq[Setting[_]] = inConfig(Native)(Seq(
        link in project := ((link in project) dependsOn (link in other)).value,
        includeDirectories in project ++= (includeDirectories in other).value,
        libraryDirectories in project += (target in other).value 
      ))
      project.settings(newSettings: _*)
    }
    
  }
  
  def NativeProject(id: String, base: File) = Project(id, base).settings(NativeDefaults.defaultSettings: _*)

}
