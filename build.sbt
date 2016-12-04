// Build settings
version in ThisBuild := ("git describe --always --dirty=-SNAPSHOT --match v[0-9].*" !!).tail.trim
crossScalaVersions in ThisBuild := List("2.11.8", "2.12.0")
scalaVersion in ThisBuild := crossScalaVersions.value.head
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-target:jvm-1.8"
)
fork in ThisBuild := true

// Publishing
organization in ThisBuild := "ch.jodersky"
licenses in ThisBuild := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause")))
homepage in ThisBuild := Some(url("https://jodersky.github.io/flow"))
publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
pomExtra in ThisBuild := {
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

// Project structure
lazy val root = (project in file("."))
  .aggregate(core, native, stream)

lazy val core = (project in file("flow-core"))
  .settings(name := "flow-core")
  .dependsOn(native % "test->runtime")

lazy val native = (project in file("flow-native"))
  .settings(name := "flow-native")

lazy val stream = (project in file("flow-stream"))
  .settings(name := "flow-stream")
  .dependsOn(core, core % "test->test", native % "test->runtime")

lazy val samplesTerminal = (project in file("flow-samples") / "terminal")
  .dependsOn(core, native % Runtime)

lazy val samplesTerminalStream = (project in file("flow-samples") / "terminal-stream")
  .dependsOn(stream, native % Runtime)

lazy val samplesWatcher = (project in file("flow-samples") / "watcher")
  .dependsOn(core, native % Runtime)

// Root project settings
publishArtifact := false
publish := ()
publishLocal := ()
// make sbt-pgp happy
publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo"))

// Generate documentation
enablePlugins(PreprocessPlugin)
sourceDirectory in Preprocess := (baseDirectory in ThisBuild).value / "Documentation"
preprocessVars in Preprocess := Map(
  "version" -> version.value,
  "native_major" -> "4",
  "native_minor" -> "0"
)

// Add scaladoc to documentation
enablePlugins(SiteScaladocPlugin)
unidocSettings
scalacOptions in (ScalaUnidoc, doc) ++= Seq(
  "-groups", // Group similar methods together based on the @group annotation.
  "-diagrams", // Show classs hierarchy diagrams (requires 'dot' to be available on path)
  "-implicits", // Add methods "inherited" through implicit conversions
  "-sourcepath", baseDirectory.value.getAbsolutePath
) ++ {
  val latestTag: String = "git describe --abbrev=0 --match v[0-9].*".!!
  Opts.doc.sourceUrl(
    s"https://github.com/jodersky/flow/blob/$latestTag€{FILE_PATH}.scala"
  )
}
siteMappings ++= (mappings in (ScalaUnidoc, packageDoc)).value.map{ case (file, path) =>
  (file, "api/" + path)
}
