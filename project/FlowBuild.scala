import sbt._
import Keys._
import JniKeys._
import UniqueVersionKeys._
import NativeKeys._


object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val ScalaVersion = "2.10.4"
  val Version = "2.0.0-RC4"
  
  
  lazy val commonSettings: Seq[Setting[_]] =
    UniqueVersionDefaults.settings ++
    Seq(
      organization := Organization,
      scalaVersion := ScalaVersion,
      baseVersion := Version,
      licenses := Seq(("BSD-3-Clause", url("http://opensource.org/licenses/BSD-3-Clause"))),
      homepage := Some(url("http://github.com/jodersky/flow")),
      resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"))

  lazy val publishSettings: Seq[Setting[_]] = Seq(
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    pomExtra := {
      <scm>
        <url>git@github.com:jodersky/flow.git</url>
        <connection>scm:git:git@github.com:jodersky/flow.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jodersky</id>
          <name>Jakob Odersky</name>
        </developer>
      </developers>
    }
  )
  
  lazy val runSettings: Seq[Setting[_]] = Seq(
    fork := true,
    connectInput in run := true,
    outputStrategy := Some(StdoutOutput)
  )

  lazy val root: Project = (
    Project("root", file(".")).aggregate(flow, flowNative)
    settings(
      publish := (),
      publishLocal := ()
    )
  )
  
  lazy val flow: Project = (
    Project("flow", file("flow"))
    settings(commonSettings: _*)
    settings(publishSettings: _*)
    settings(JniDefaults.settings: _*)
    settings(
      javahHeaderDirectory := (baseDirectory in ThisBuild).value / "flow-native" / "src",
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      compileOrder in Compile := CompileOrder.Mixed,
      libraryDependencies += Dependencies.akkaActor,
      libraryDependencies += Dependencies.ioCore,
      libraryDependencies += Dependencies.ioFile
    )
  )

  lazy val flowNative: Project = (
    Project("flow-native", file("flow-native-scala"))
    settings(commonSettings: _*)
    settings(publishSettings: _*)
    settings(NativeDefaults.settings: _*)
    settings(
      nativeBuildDirectory := (baseDirectory in ThisBuild).value / "flow-native"
    )
  )

  lazy val samplesTerminal = (
    Project("flow-samples-terminal", file("flow-samples") / "flow-samples-terminal")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(flow)
  )

}
