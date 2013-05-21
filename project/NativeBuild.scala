import sbt._
import Keys._

object NativeBuild {
  
  //settings
  val sourceDirectory = SettingKey[File]("native-source-directory", "Native source directory containing files to compile.")
  val compiler = SettingKey[String]("native-compiler", "Native compiler.")
  val options = SettingKey[Seq[String]]("native-options", "Flags for native compiler.")
  val includeDirectories = SettingKey[Seq[File]]("native-include-directories", "Include directories for native compiler.")
  val outputDirectory = SettingKey[File]("native-output-directory", "Directory for native output.")
  val linker = SettingKey[String]("native-linker", "Native linker.")
  val linkerOutput = SettingKey[File]("native-linker-output", "Name of linker output.")
  val linkerOptions = SettingKey[Seq[String]]("native-linker-options", "Native linker options.")
  val linkerLibraries = SettingKey[Seq[String]]("native-linker-libraries", "Libraries against which to link.")
  
  //tasks
  val outputFromSource = TaskKey[File => File]("native-output-from-source", "Get name of native binary from source file.")
  val sources = TaskKey[Seq[File]]("native-source", "Native source files to compile.")
  val makeOutputDirectory = TaskKey[Unit]("native-make-output-directory", "Make native output directory.")
  val compile = TaskKey[Unit]("native-compile", "Compiles native sources.")
  val link = TaskKey[Unit]("native-link", "Link native sources.")
  
  //task implementations
  val outputFromSourceTask = outputFromSource <<= (outputDirectory) map {
    outputDir => 
      ((src: File) => {file((outputDir / src.base).absolutePath + ".o")})
  }
  
  val makeOutputDirectoryTask = makeOutputDirectory <<= (outputDirectory) map {o => o.mkdirs(); {}}
  
  def compileSingleFile(compiler: String, options: Seq[String], includeDirectories: Seq[File], source: File, s2o: File => File): Unit = {
    val cmdParts =
      List(compiler) ++
        options ++
        includeDirectories.map(i => "-I" + i.absolutePath) ++
        List("-c", source.absolutePath) ++
        List("-o", s2o(source))

    val cmd = cmdParts.mkString(" ")
    cmd !
  }

  val compileTask = compile <<= (compiler, options, includeDirectories, sources, outputFromSource) map {
    (c, f, i, srcs, out) => for (s <- srcs) compileSingleFile(c,f,i,s,out)
  } dependsOn(makeOutputDirectory)
  
  val linkTask = link <<= (linker, linkerOptions, linkerLibraries, linkerOutput, sources, outputFromSource) map { (l, opts, libs, out, srcs, s2o) =>
      val outs = srcs.map(s2o(_))
      val cmd: Seq[String] = Seq(l) ++ opts ++ Seq("-o", out.absolutePath) ++ outs.map(_.absolutePath) ++ libs.map(lib => "-l" + lib)
      cmd !;
      {} 
  } dependsOn(compile)
  


  lazy val defaults = Seq(
    compiler := "gcc",
    options := Seq(),
    sourceDirectory <<= Keys.sourceDirectory(_ / "main" / "c"),
    sources <<= sourceDirectory map (dir => (dir ** "*.c").get),
    includeDirectories <<= sourceDirectory(dir => Seq(dir)),
    outputDirectory <<= target(_ / "c"),
    linker := "gcc",
    linkerOutput <<= outputDirectory(_ / "a.out"),
    linkerOptions := Seq(),
    linkerLibraries := Seq(),
    
    outputFromSourceTask,
    makeOutputDirectoryTask,
    compileTask,
    linkTask
    )

}