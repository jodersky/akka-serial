// Build settings
version in ThisBuild := {
  import scala.sys.process._
  ("git describe --always --dirty=-SNAPSHOT --match v[0-9].*" !!).tail.trim
}
crossScalaVersions in ThisBuild := List("2.12.4", "2.11.11")
scalaVersion in ThisBuild := crossScalaVersions.value.head
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-target:jvm-1.8"
)
fork in ThisBuild := true
connectInput in run in ThisBuild := true
outputStrategy in run in ThisBuild := Some(StdoutOutput)

// Publishing
organization in ThisBuild := "ch.jodersky"
licenses in ThisBuild := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause")))
homepage in ThisBuild := Some(url("https://jodersky.github.io/akka-serial"))
publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
pomExtra in ThisBuild := {
  <scm>
    <url>git@github.com:jodersky/akka-serial.git</url>
    <connection>scm:git:git@github.com:jodersky/akka-serial.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jodersky</id>
      <name>Jakob Odersky</name>
    </developer>
  </developers>
}

// Project structure
lazy val root = (project in file("."))
  .aggregate(core, native, stream, sync)

lazy val core = (project in file("core"))
  .settings(name := "akka-serial-core")
  .dependsOn(sync, sync % "test->test")

lazy val native = (project in file("native"))
  .settings(name := "akka-serial-native")

lazy val stream = (project in file("stream"))
  .settings(name := "akka-serial-stream")
  .dependsOn(core, sync % "test->test")

lazy val sync = (project in file("sync"))
  .settings(name := "akka-serial-sync")
  .dependsOn(native % "test->runtime")

lazy val samplesTerminal = (project in file("samples") / "terminal")
  .dependsOn(core, native % Runtime)

lazy val samplesTerminalStream = (project in file("samples") / "terminal-stream")
  .dependsOn(stream, native % Runtime)

lazy val samplesWatcher = (project in file("samples") / "watcher")
  .dependsOn(core, native % Runtime)

// Root project settings
publishArtifact := false
publish := {}
publishLocal := {}
// make sbt-pgp happy
publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo"))

// Generate documentation
enablePlugins(PreprocessPlugin)
sourceDirectory in Preprocess := (baseDirectory in ThisBuild).value / "Documentation"
preprocessVars in Preprocess := Map(
  "version" -> version.value,
  "native_major" -> "1",
  "native_minor" -> "0"
)

// Add scaladoc to documentation
enablePlugins(SiteScaladocPlugin)
enablePlugins(ScalaUnidocPlugin)
unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(
  samplesTerminal, samplesTerminalStream, samplesWatcher)
scalacOptions in (ScalaUnidoc, doc) ++= Seq(
  "-groups", // Group similar methods together based on the @group annotation.
  "-diagrams", // Show classs hierarchy diagrams (requires 'dot' to be available on path)
  "-implicits", // Add methods "inherited" through implicit conversions
  "-sourcepath", baseDirectory.value.getAbsolutePath
) ++ {
  import scala.sys.process._
  val latestTag: String = "git describe --abbrev=0 --match v[0-9].*".!!
  Opts.doc.sourceUrl(
    s"https://github.com/jodersky/akka-serial/blob/$latestTag€{FILE_PATH}.scala"
  )
}
siteMappings ++= (mappings in (ScalaUnidoc, packageDoc)).value.map{ case (file, path) =>
  (file, "api/" + path)
}
