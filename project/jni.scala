import sbt._
import Keys._
import NativeKeys._

object JniKeys {
  val jdkHome = settingKey[File]("Home of JDK.")
  val javahHeaderDirectory = settingKey[File]("Directory where generated javah header files are placed.")
  val javahClasses = settingKey[Seq[String]]("Fully qualified names of classes containing native declarations.")
  val javahClasspath = taskKey[Seq[File]]("Classpath to use in javah.")
  val javah = taskKey[Seq[File]]("Generate JNI headers.")
}

object JniDefaults {
  import JniKeys._

  val defaultSettings: Seq[Setting[_]] = Seq(
    jdkHome := file(sys.env("JAVA_HOME")),
    javahHeaderDirectory := (sourceManaged in Native).value / "javah",
    javah := javahImpl.value, 
    sourceGenerators in Native <+= javah map { headers => headers},
    includeDirectories in Native += javahHeaderDirectory.value,
    includeDirectories in Native += jdkHome.value / "include")

  def javahImpl = Def.task {
    val cps = javahClasspath.value
    val cp = cps.mkString(":")
    for (clazz <- javahClasses.value) {
      val parts = Seq(
        "javah",
        "-d", javahHeaderDirectory.value,
        "-classpath", cp,
        clazz)
      val cmd = parts.mkString(" ")
      val ev = Process(cmd) ! streams.value.log
      if (ev != 0) throw new RuntimeException("Error occured running javah.")
    }
    IO.listFiles(javahHeaderDirectory.value)
  }
  
}

