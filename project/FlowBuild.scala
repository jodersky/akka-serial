package flow

import sbt._
import Keys._

object FlowBuild extends Build {

  val scalaVersions = List("2.11.8", "2.12.0-M3")

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    resolvers += Resolver.jcenterRepo,
    scalaVersion in ThisBuild := scalaVersions.head,
    crossScalaVersions in ThisBuild := scalaVersions.reverse,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-target:jvm-1.8"),
    organization := "com.github.jodersky",
    licenses := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))),
    homepage := Some(url("https://jodersky.github.io/flow")),
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

  lazy val root: Project = (
    Project("root", file("."))
    aggregate(core, native, stream)
    settings(commonSettings: _*)
    settings(
      publishArtifact := false,
      publish := (),
      publishLocal := (),
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")) // make sbt-pgp happy
    )
  )

  lazy val core = Project(
    id = "flow-core",
    base = file("flow-core")
  )

  lazy val native = Project(
    id = "flow-native",
    base = file("flow-native")
  )

  lazy val stream = Project(
    id = "flow-stream",
    base = file("flow-stream"),
    dependencies = Seq(core)
  )

  lazy val samplesTerminal = Project(
    id = "samples-terminal",
    base = file("flow-samples") / "terminal",
    dependencies = Seq(core, native % Runtime)
  )

  lazy val samplesTerminalStream = Project(
    id = "samples-terminal-stream",
    base = file("flow-samples") / "terminal-stream",
    dependencies = Seq(stream, native % Runtime)
  )

  lazy val samplesWatcher = Project(
    id = "samples-watcher",
    base = file("flow-samples") / "watcher",
    dependencies = Seq(core, native % Runtime)
  )

}
