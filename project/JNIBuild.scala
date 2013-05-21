import sbt._
import Keys._

object JNIBuild {
  val jdkHome = SettingKey[File]("jdk-home", "Home of JDK.")
  val javaClass = SettingKey[String]("jni-class", "Fully qualified name of class containing native declarations.")

  val javah = TaskKey[Unit]("javah", "Generate JNI headers.")

  val javahTask = javah <<= (javaClass, NativeBuild.sourceDirectory, Keys.classDirectory in Compile) map { (j, src, cp) =>
    val cmd = "javah -d " + src.absolutePath + " -classpath " + cp.absolutePath + " " + j
    cmd !;
    {}
  } dependsOn (Keys.compile in Compile)

  val defaults: Seq[Setting[_]] = NativeBuild.defaults ++ Seq(
    javahTask,
    NativeBuild.compile <<= NativeBuild.compile.dependsOn(javah)
    )

}

