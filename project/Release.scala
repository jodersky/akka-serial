package flow

import sbt._
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

import ch.jodersky.sbt.jni.plugins.JniNative.autoImport._
import ch.jodersky.sbt.jni.plugins.JniPackaging.autoImport._

import com.typesafe.sbt.pgp.PgpKeys._

object Release {


  def settings: Seq[Setting[_]] = Seq(

    //sign git tags
    releaseVcs := Some(new SignedGit(Keys.baseDirectory.value)),

    //publish signed
    releasePublishArtifactsAction := publishSigned.value,

    //build for multiple scala versions,
    releaseCrossBuild := true,

    releaseProcess := Seq[ReleaseStep](

      //Check that there are no snapshot dependencies
      checkSnapshotDependencies,

      //During a release, only native libraries in lib_native will be packaged
      disableLocalBuild,

      //Check that there are native libraries in lib_native and list all
      //libraries that will be packaged
      checkNativeLibs,

      //Ask for release version and next development version
      inquireVersions,

      //Set version to release version and save
      setReleaseVersion,

      //Clean
      runClean,

      //Compile and test
      runTest,

      //If all tests pass, commit the updated version
      commitReleaseVersion,

      //Also create a tag
      tagRelease,

      //Publish artifacts, note that they will only be uploaded, not yet be released to the public
      publishArtifacts,

      //Bump version to next development
      setNextVersion,

      //TODO: update website

      //Commit
      commitNextVersion,

      //Push all changes (commits and tags) to GitHub
      pushChanges

      //TODO: release artifact on bintray
    )
  )

  /** Set `enableNativeCompilations` to false. */
  lazy val disableLocalBuild = ReleaseStep(st => {
    val st1 = ReleaseStateTransformations.reapply(Seq(
      enableNativeCompilation in FlowBuild.native in Compile := false
    ), st)
    st1.log.info("Disabled compilation of native libraries during release process.")
    st1
  })

  /** Release step that prints all native libraries that will be packaged
    * and awaits approval from user. */
  lazy val checkNativeLibs = ReleaseStep(action = st0 => {
    val log = st0.log
    val project = FlowBuild.native

    val extracted = Project.extract(st0)
    val (st1, libs) = extracted.runTask(unmanagedNativeLibraries in project in Compile, st0)

    log.info("The following native libraries will be packaged:")
    log.info("Kernel\tArchitecture\tFile")
    log.info("---------------------")
    libs.toSeq.sortBy(_._1.id).foreach{ case (platform, file) =>
      log.info(platform.kernel + "\t" + platform.arch + "\t" + file.getAbsolutePath)
    }

    val currentPlatform = extracted.get(nativePlatform in project in Compile)
    if (!libs.contains(currentPlatform)) {
      log.warn("Native library for the current platform does not exist! It will not be released.")
    }
    SimpleReader.readLine("Are the all native libraries listed (y/n)? [n] ") match {
      case Some("y") => //do nothing
      case _ => sys.error("Mssing native libaries. Aborting release.")
    }
    st1
  })

  /** A Git wrapper that signs tags. */
  class SignedGit(baseDir: File) extends Git(baseDir) {
    override def tag(name: String, comment: String, force: Boolean = false) =
      cmd("tag", "-s", name, "-m", comment, if(force) "-f" else "")
  }

}
