import sbt._
import Keys._

import com.github.jodersky.build.NativeKeys._
import com.github.jodersky.build.NativePlugin._
import com.github.jodersky.build.NativeDefault
import Jni._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val Version = "1.0-SNAPSHOT"
  val ScalaVersion = "2.10.1"
  
  lazy val main: Project = Project("flow", file("."), settings = 
    Defaults.defaultSettings ++
    buildSettings ++ 
    NativeDefault.defaultSettings ++
    Seq(
      libraryDependencies ++= Dependencies.all,
      javahClasses := Seq("com.github.jodersky.flow.low.NativeSerial"),
      includeDirectories in Native += jdkHome.value / "include" / "linux",
      binaryType in Native := SharedLibrary,
      binaryName in Native := "flow",
      options in Native := Seq("-fPIC", "-O2"),
      linkOptions in Native := Seq("-Wl,-soname,libflow.so.1"),
      resourceGenerators in Compile <+= (resourceManaged in Compile, link in Native) map { (resDir, binary) =>
        val file = resDir / "native" / sys.props("os.name").toLowerCase / sys.props("os.arch").toLowerCase / binary.getName
        IO.copyFile(binary, file)
        Seq(file)
      }
    ) ++ Jni.defaultSettings)
    
  lazy val samples = Project(
    id = "flow-rwc",
    base = file("samples") / "rwc",
    settings = buildSettings ++ runSettings ++ Seq(libraryDependencies ++= Dependencies.all)).dependsOn(main)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    compileOrder in Compile := CompileOrder.Mixed)

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
