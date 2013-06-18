import sbt._
import Keys._

import com.github.jodersky.build.NativeKeys._
import com.github.jodersky.build.NativePlugin._
import Jni._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val Version = "1.0-SNAPSHOT"
  val ScalaVersion = "2.10.1"

  lazy val main = Project("flow-main", file("flow-main")).settings(
    buildSettings ++ Seq(libraryDependencies ++= Dependencies.all): _*
  )
    
  lazy val native = NativeProject("flow-native", file("flow-native")).settings((Seq(
    javahClasses := Seq("com.github.jodersky.flow.low.NativeSerial"),
    includeDirectories in Native += jdkHome.value / "include" / "linux",
    nativeSource in Native := baseDirectory.value / "src",
    binaryType in Native := SharedLibrary,
    binaryName in Native := "flow",
    options in Native := Seq("-fPIC", "-O2"),
    linkOptions in Native := Seq("-Wl,-soname,libflow.so.1")
    ) ++ Jni.defaultSettings): _*).dependsOn(main)
    
  lazy val example = Project(
    id = "flow-example",
    base = file("example"),
    settings = buildSettings ++ runSettings ++ Seq(libraryDependencies ++= Dependencies.all))

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
