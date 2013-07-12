import sbt._
import Keys._
import NativeKeys._
import scala.util.Try

object JniKeys {
  val jdkHome = settingKey[Option[File]]("Home of JDK.")
  val jdkHomeNotFound = taskKey[Unit]("Utility task that informs user that no JDK was found.")
  val javahHeaderDirectory = settingKey[File]("Directory where generated javah header files are placed.")
  val javahClasses = settingKey[Seq[String]]("Fully qualified names of classes containing native declarations.")
  val javahClasspath = taskKey[Seq[File]]("Classpath to use in javah.")
  val javah = taskKey[Seq[File]]("Generate JNI headers.")
}

object JniDefaults {
  import JniKeys._

  val defaultSettings: Seq[Setting[_]] = Seq(
    jdkHome := Try(file(sys.env("JAVA_HOME"))).toOption,
    jdkHomeNotFound := jdkHomeNotFoundImpl.value,
    nativeCompile in Native := ((nativeCompile in Native) dependsOn jdkHomeNotFound).value,
    javahHeaderDirectory := (sourceManaged in Native).value / "javah",
    javah := javahImpl.value,
    sourceGenerators in Native <+= javah map { headers => headers},
    includeDirectories in Native += javahHeaderDirectory.value,
    includeDirectories in Native ++= jdkHome.value.map( jdk => jdk / "include").toSeq)
    
  def jdkHomeNotFoundImpl = Def.task {
    if (jdkHome.value == None) {
      streams.value.log.warn(
        "No JDK home directory found, any native code using JNI may not compile. Please set JAVA_HOME environment variable.")
    }
    ()
  } 

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

