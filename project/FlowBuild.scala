import sbt._
import Keys._
import JniKeys._
import NativeKeys._
import NativePackKeys._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val ScalaVersion = "2.10.3"
  val Version = "1.1.0" //version of flow library
  val NativeMajorVersion = 2 //major version of native API
  val NativeMinorVersionPosix = 0 //minor version of native posix implementation
  val NativeVersionPosix = NativeMajorVersion + "." + NativeMinorVersionPosix
  
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
    Project("root", file(".")).aggregate(flow, flowPack)
    settings(
      publish := (),
      publishLocal := ()
    )
  )
  
  lazy val flow: Project = (
    Project("flow", file("flow"))
    settings (commonSettings: _*)
    settings (JniDefaults.settings: _*)
    settings (NativeDefaults.settings: _*)
    settings (selectHost().settings: _*)
    settings(
      nativeIncludeDirectories += (sourceDirectory in Compile).value / "native" / "include",
      nativeIncludeDirectories ++= jdkHome.value.map(jdk => jdk / "include").toSeq,
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      javahHeaderDirectory := (sourceDirectory in Compile).value / "native" / "include",
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
        Dependencies.akkaActor,
        Dependencies.ioCore,
        Dependencies.ioFile)
    )
  )

  //the current operating system used to run the native compile
  trait Host{ def settings: Seq[Setting[_]] }

  object Linux extends Host {

    val compiler = "gcc"
    val linker = compiler
    val cFlags = List("-O2", "-fPIC")
    val linkerFlags = List("-shared", s"-Wl,-soname,libflow.so.${NativeMajorVersion}")
    val binary = "libflow.so"

    val builds = List(
      NativeBuild("x86_64-linux-gnu", "gcc", "-m64" :: cFlags, "gcc", "-m64" :: linkerFlags, binary),
      NativeBuild("x86-linux-gnu", "gcc", "-m32" :: cFlags, "gcc", "-m32" :: linkerFlags, binary),
      NativeBuild("arm-linux-gnueabihf", "arm-linux-gnueabihf-gcc", cFlags, "arm-linux-gnueabihf-gcc", linkerFlags, binary),
      NativeBuild("arm-linux-gnueabi", "arm-linux-gnueabi-gcc", cFlags, "arm-linux-gnueabi-gcc", linkerFlags, binary)
      //add other build configurations here or adapt existing ones to your needs
    )

    lazy val settings = Seq(
      nativeVersion := NativeVersionPosix,
      nativeIncludeDirectories ++= jdkHome.value.map(jdk => jdk / "include" / "linux").toSeq,
      nativeSource := nativeSource.value / "posix",
      nativeBuilds := builds
    )

  }

  //stub, not sure if this works
  object MacOSX extends Host {
      
    val compiler = "gcc"
    val linker = compiler
    val cFlags = Seq("-O2", "-fPIC")
    val linkerFlags = Seq("-dynamiclib")
    val binary = s"libflow.jnilib"

    val localBuild = NativeBuild(
      "amd64-macosx",
      compiler,
      cFlags,
      linker,
      linkerFlags,
      binary
    )

    lazy val settings = Seq(
      nativeVersion := NativeVersionPosix,
      nativeIncludeDirectories += file("/System/Library/Frameworks/JavaVM.framework/Headers/jni.h"),
      nativeIncludeDirectories += file("/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers"),
      nativeSource := nativeSource.value / "posix",
      nativeBuilds := Seq(localBuild)
    )

  }

  private def osError = throw new RuntimeException("Sorry, native compilation under the current OS is not supported.")
  def selectHost() = System.getProperty("os.name").toLowerCase match {
    case "linux" => Linux
    case "macosx" => MacOSX
    case _ => new Host{
      val settings = Seq(
        nativeCompile := osError,
        nativeLink := osError
      ) 
    }
  }

  lazy val flowPack: Project = (
    Project("flow-pack", file("flow-pack"))
    settings (commonSettings: _*)
    settings (NativePackDefaults.settings: _*)
    settings (
      nativePackLinkages := {
        val linkMappings = Map(
          "x86_64-linux-gnu" -> "amd64-linux",
          "x86-linux-gnu" -> "x86-linux",
          "arm-linux-gnueabihf" -> "arm-linux"
        )
        val ls: Seq[(NativeBuild, File)] = (nativeLink in flow).value.toSeq
        for ((build, binary) <- ls; n <- linkMappings.get(build.name)) yield {
          (build.copy(name = n), binary)
        }
      },
      exportJars := true
    )
    dependsOn(flow)
  )

  lazy val samplesTerminal = (
    Project("flow-samples-terminal", file("flow-samples") / "flow-samples-terminal")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(flowPack)
  )


}
