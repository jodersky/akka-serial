import sbt._
import Keys._
import scala.util.Try

object JniKeys {
  val javahHeaderDirectory = settingKey[File]("Directory where generated javah header files are placed.")
  val javahClasses = settingKey[Seq[String]]("Fully qualified names of classes containing native declarations.")
  val javahClasspath = taskKey[Seq[File]]("Classpath to use in javah.")
  val javah = taskKey[Seq[File]]("Generate JNI headers.")
}

object JniDefaults {
  import JniKeys._

  val settings: Seq[Setting[_]] = Seq(
    javahHeaderDirectory := baseDirectory.value,
    javahClasspath := Seq((classDirectory in Compile).value),
    javah := javahImpl.value
  )
    
  def javahImpl = Def.task {
    val jcp = javahClasspath.value
    val cp = jcp.mkString(sys.props("path.separator"))
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

