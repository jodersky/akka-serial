import sbt._
import Keys._
import JniKeys._
import NativeKeys._
import Publish._


object FlowBuild extends Build {
  
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    version := "2.1.2-SNAPSHOT",
    scalaVersion in ThisBuild := "2.11.6",
    crossScalaVersions in ThisBuild := Seq("2.10.5", "2.11.6"),
    organization := "com.github.jodersky",
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
  )
  
  lazy val runSettings: Seq[Setting[_]] = Seq(
    fork := true,
    connectInput in run := true,
    outputStrategy := Some(StdoutOutput)
  )

  lazy val root: Project = (
    Project("root", file("."))
    aggregate(main, native)
    settings(publishSettings: _*)  
    settings(
      publishArtifact := false,
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")) // make sbt-pgp happy
    )
  )
  
  lazy val main: Project = (
    Project("main", file("flow-main"))
    settings(commonSettings: _*)
    settings(publishSettings: _*)
    settings(JniDefaults.settings: _*)
    settings(
      name := "flow",
      javahHeaderDirectory := (baseDirectory in ThisBuild).value / "flow-native" / "src",
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      compileOrder in Compile := CompileOrder.Mixed,
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.10"
    )
  )

  lazy val native: Project = (
    Project("native", file("flow-native-sbt"))
    settings(commonSettings: _*)
    settings(publishSettings: _*)
    settings(NativeDefaults.settings: _*)
    settings(
      name := "flow-native",
      crossPaths := false,
      nativeBuildDirectory := (baseDirectory in ThisBuild).value / "flow-native"
    )
  )

  lazy val samplesTerminal = (
    Project("samples-terminal", file("flow-samples") / "terminal")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(main)

    //kind of dirty, but it gets the sample to run without installing native libraries
    settings(
      (run in Compile) <<= (run in Compile).dependsOn(nativeBuild in native),
      javaOptions += "-Djava.library.path=" + (nativeOutputDirectory in native).value.getAbsolutePath()
    )    
  )

}
