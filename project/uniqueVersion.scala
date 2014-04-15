import sbt._
import Keys._

object UniqueVersionKeys {

  val gitHeadCommitSha = settingKey[String]("Current commit sha.")
  val baseVersion = settingKey[String]("Base part of version, string without unique hash appended.")
  val isRelease = settingKey[Boolean]("Is this a release? Should the unique hash be appended to the version string?")

}

object UniqueVersionDefaults {
  import UniqueVersionKeys._

  lazy val settings: Seq[Setting[_]] = Seq(
    gitHeadCommitSha := Process("git rev-parse HEAD").lines.head,
    isRelease := sys.props("release") == "true",
    version := { if (isRelease.value) baseVersion.value else baseVersion.value + "-" + gitHeadCommitSha.value }
  )

}