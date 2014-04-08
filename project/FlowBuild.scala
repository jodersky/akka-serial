import sbt._
import Keys._
import JniKeys._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val ScalaVersion = "2.10.3"
  val Version = "2.0.0-RC1" //version of flow library
  
  val gitHeadCommitSha = settingKey[String]("Current commit sha.")
  
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := Organization,
    scalaVersion := ScalaVersion,
    isSnapshot := sys.props("release") != "true",
    gitHeadCommitSha in ThisBuild := Process("git rev-parse HEAD").lines.head,
    version in ThisBuild:= { if (!isSnapshot.value) Version else Version + "-" + gitHeadCommitSha.value },
    licenses := Seq(("BSD-3-Clause", url("http://opensource.org/licenses/BSD-3-Clause"))),
    homepage := Some(url("http://github.com/jodersky/flow")),
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"))
  
  lazy val runSettings: Seq[Setting[_]] = Seq(
    fork := true,
    connectInput in run := true,
    outputStrategy := Some(StdoutOutput)
  )

  lazy val root: Project = (
    Project("root", file(".")).aggregate(flow)
    settings(
      publish := (),
      publishLocal := ()
    )
  )
  
  lazy val flow: Project = (
    Project("flow", file("flow"))
    settings (commonSettings: _*)
    settings (JniDefaults.settings: _*)
    settings(
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      javahHeaderDirectory := (baseDirectory in ThisBuild).value / "flow-native" / "include",
      compileOrder in Compile := CompileOrder.Mixed,
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
      },
      libraryDependencies ++= Seq(
        Dependencies.akkaActor
      )
    )
  )

  lazy val samplesTerminal = (
    Project("flow-samples-terminal", file("flow-samples") / "flow-samples-terminal")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    //dependsOn(flowPack)
    dependsOn(flow)
  )

}
