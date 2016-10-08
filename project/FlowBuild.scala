package flow

import sbt._
import Keys._

object FlowBuild extends Build {

  val scalaVersions = List("2.11.8", "2.12.0-RC1")

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    resolvers += Resolver.jcenterRepo,
    scalaVersion := scalaVersions.head,
    crossScalaVersions := scalaVersions.reverse,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-target:jvm-1.8"),
    organization := "ch.jodersky",
    licenses := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))),
    homepage := Some(url("https://jodersky.github.io/flow")),
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
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

  lazy val root = (project in file(".")).
    aggregate(core, native, stream)

  lazy val core = (project in file("flow-core")).
    settings(name:= "flow-core")

  lazy val native = (project in file("flow-native")).
    settings(name:= "flow-native")

  lazy val stream = (project in file("flow-stream")).
    settings(name:= "flow-stream").
    dependsOn(core)

  lazy val samplesTerminal = (project in file("flow-samples") / "terminal").
    dependsOn(core, native % Runtime)

  lazy val samplesTerminalStream = (project in file("flow-samples") / "terminal-stream").
    dependsOn(stream, native % Runtime)

  lazy val samplesWatcher = (project in file("flow-samples") / "watcher").
    dependsOn(core, native % Runtime)

}
