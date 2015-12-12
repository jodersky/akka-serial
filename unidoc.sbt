import sbtunidoc.Plugin.UnidocKeys._

unidocSettings

scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
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
