import flow.{FlowBuild, Release}

FlowBuild.commonSettings

Release.settings

/* Settings related to publishing */
publishArtifact := false
publish := ()
publishLocal := ()
// make sbt-pgp happy
publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo"))

/* Generate documentation */
enablePlugins(PreprocessPlugin)
sourceDirectory in Preprocess := (baseDirectory in ThisBuild).value / "Documentation"
preprocessVars in Preprocess := Map(
  "version" -> version.value,
  "native_major" -> "3",
  "native_minor" -> "0"
)

/* Add scaladoc to documentation */
enablePlugins(SiteScaladocPlugin)
unidocSettings
scalacOptions in (ScalaUnidoc, doc) ++= Seq(
  "-groups", // Group similar methods together based on the @group annotation.
  "-diagrams", // Show classs hierarchy diagrams (requires 'dot' to be available on path)
  "-implicits", // Add methods "inherited" through implicit conversions
  "-sourcepath", baseDirectory.value.getAbsolutePath
) ++ {
  val latestTag: String = "git describe --abbrev=0".!!
  Opts.doc.sourceUrl(
  s"https://github.com/jodersky/flow/blob/$latestTag€{FILE_PATH}.scala"
  )
}
siteMappings ++= (mappings in (ScalaUnidoc, packageDoc)).value.map{ case (file, path) =>
  (file, "api/" + path)
}
