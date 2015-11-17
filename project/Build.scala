import JniKeys._
import native.NativeDefaults
import native.NativeKeys._
import sbt._
import sbt.Keys._

object FlowBuild extends Build {

  val scalaVersions = List("2.11.7", "2.12.0-M2")

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    version := "2.4.0-SNAPSHOT",
    scalaVersion in ThisBuild := scalaVersions.head,
    crossScalaVersions in ThisBuild := scalaVersions.reverse,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-target:jvm-1.8"),
    organization := "com.github.jodersky",
    licenses := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))),
    homepage := Some(url("https://github.com/jodersky/flow")),
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
    Project("root", file("."))
    aggregate(main, native)
    settings(commonSettings: _*)
    settings(
      publishArtifact := false,
      publish := (),
      publishLocal := (),
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")) // make sbt-pgp happy
    )
  )

  lazy val main: Project = (
    Project("flow-main", file("flow-main"))
    settings(commonSettings: _*)
    settings(JniDefaults.settings: _*)
    settings(
      name := "flow",
      javahHeaderDirectory := (baseDirectory in ThisBuild).value / "flow-native" / "src",
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      compileOrder in Compile := CompileOrder.Mixed,
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"
    )
  )

  lazy val native: Project = (
    Project("flow-native", file("flow-native-sbt"))
    settings(commonSettings: _*)
    settings(NativeDefaults.settings: _*)
    settings(
      name := "flow-native",
      crossPaths := false,
      libraryPrefix in Compile := "com/github/jodersky/flow/native",
      sourceDirectory in Native := (baseDirectory in ThisBuild).value / "flow-native"
    )
  )

  lazy val samplesTerminal = (
    Project("flow-samples-terminal", file("flow-samples") / "terminal")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(main)
    dependsOn(native)
  )

  lazy val samplesWatcher = (
    Project("flow-samples-watcher", file("flow-samples") / "watcher")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(main)
  )

}
