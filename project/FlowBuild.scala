package flow

import sbt._
import Keys._

object FlowBuild extends Build {

  val scalaVersions = List("2.11.7", "2.12.0-M3")

  lazy val commonSettings: Seq[Setting[_]] = Seq(
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
    aggregate(main, native)
    settings(commonSettings: _*)
    settings(
      publishArtifact := false,
      publish := (),
      publishLocal := (),
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")) // make sbt-pgp happy
    )
  )

  lazy val main = Project(
    id = "flow-main",
    base = file("flow-main")
  )

  lazy val native = Project(
    id = "flow-native",
    base = file("flow-native")
  )

  lazy val samplesTerminal = Project(
    id = "flow-samples-terminal",
    base = file("flow-samples") / "terminal",
    dependencies = Seq(main, native % Runtime)
  )

  lazy val samplesWatcher = Project(
    id = "flow-samples-watcher",
    base = file("flow-samples") / "watcher",
    dependencies = Seq(main, native % Runtime)
  )

}
