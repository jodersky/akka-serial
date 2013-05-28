import sbt._
import Keys._
import NativeBuild._
import JNIBuild._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val Version = "1.0-SNAPSHOT"
  val ScalaVersion = "2.10.1"

  lazy val root = Project(
    id = "flow",
    base = file("main"),
    settings = buildSettings ++ jniSettings ++ Seq(libraryDependencies ++= Dependencies.all))
    
  lazy val example = Project(
    id = "flow-example",
    base = file("example"),
    settings = buildSettings ++ runSettings ++ Seq(libraryDependencies ++= Dependencies.all)).dependsOn(root)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    compileOrder in Compile := CompileOrder.Mixed)

  lazy val jniSettings = JNIBuild.defaults ++ Seq(
    jdkHome := file(System.getProperty("java.home")) / "..",
    javaClass := "com.github.jodersky.flow.low.NativeSerial",
    NativeBuild.compiler := "gcc",
    options := Seq("-fPIC"),
    NativeBuild.includeDirectories <<= jdkHome apply (jdk => Seq(jdk / "include", jdk / "include" / "linux")),
    linker := "gcc",
    linkerOptions := Seq("-shared", "-Wl,-soname,libflow.so.1"),
    linkerOutput <<= NativeBuild.outputDirectory(_ / "libflow.so"),
    Keys.packageBin in Compile <<= (Keys.packageBin in Compile).dependsOn(NativeBuild.link),
    mappings in (Compile, packageBin) <+= linkerOutput map { out =>
      out -> ("native/" + System.getProperty("os.name").toLowerCase + "/" + System.getProperty("os.arch").toLowerCase + "/libflow.so")
    },
    exportJars := true
  )

  lazy val runSettings = Seq(
    fork := true,
    connectInput in run := true,
    javaOptions in run += "-Djava.library.path=.")
}

object Dependencies {

  lazy val io = "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"
  lazy val file = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.2-M3"
  
  lazy val all = Seq(akka)

}
