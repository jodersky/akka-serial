import sbt._
import Keys._

import com.github.jodersky.build.NativeKeys._
import com.github.jodersky.build.NativePlugin._
import com.github.jodersky.build.NativeDefault
import Jni._

object FlowBuild extends Build {
  val Organization = "com.github.jodersky"
  val Version = "1.0-SNAPSHOT" //version of flow library
  val BinaryMajorVersion = 1 //binary major version used to select so's and dlls when publishing (needs to be incremented if API changes are made to flow.h or NativeSerial.java)
  val ScalaVersion = "2.10.1"
  //see native settings down below
  
  lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization := Organization,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"))
  
  lazy val runSettings: Seq[Setting[_]] = Seq(
    fork := true,
    connectInput in run := true)
      
  lazy val main: Project = (
    Project("flow-main", file("flow-main"))
    settings (commonSettings: _*)
    settings (
      libraryDependencies ++= Seq(
        Dependencies.akkaActor,
        Dependencies.ioCore,
        Dependencies.ioFile), 
      compileOrder in Compile := CompileOrder.Mixed,
      resourceGenerators in Compile <+= (resourceManaged in Compile, link in Native in getOsProject) map { (resDir, binary) =>
      	val file = canonicalBinaryPath(resDir / "native", binary.getName)
        IO.copyFile(binary, file)
        Seq(file)
      }
    )
  )
  
  lazy val rwc = (
    Project("flow-samples-rwc", file("flow-samples") / "rwc")
    settings(commonSettings: _*)
    settings(runSettings: _*)
    dependsOn(main)
  )

 
  //--- native settings --------------------------------------------------
  
  def getOsProject = {
    sys.props("os.name").toLowerCase match {
      case "linux" => LocalProject("flow-native-linux") //use local project to avoid stackoverflow on cyclic dependencies
      case _ => throw new Exception("There is no native project defined for your current OS." +
        " Have a look at the project flow-native-linux to see how to create one yourself. You may ignore this error by commenting " +
        "the relevant lines in project/Build.scala, however by doing so, compiling native sources may miserably fail!")
    }
  }
  
  def canonicalBinaryPath(base: File, binaryName: String) = {
	base / sys.props("os.name").toLowerCase / sys.props("os.arch").toLowerCase / binaryName    
  } 
  
  val publishNative = taskKey[File]("Publish native binary compiled on current OS to flow-binaries project so that it may be packaged in a distribution of flow.")
  val publishNativeImpl = Def.task{
    val in = (link in Native).value
    val out = canonicalBinaryPath((baseDirectory in ThisBuild).value / "flow-binaries", in.getName)
    IO.copyFile(in, out)
    out
  }

  lazy val commonNativeSettings: Seq[Setting[_]] = Seq(
    includeDirectories in Native += file("flow-native") / "shared" / "include",
    nativeCompile in Native := ((nativeCompile in Native) dependsOn (compile in Compile in main)).value,
    publishNative := publishNativeImpl.value,
    javahClasspath := Seq((classDirectory in Compile in main).value),
    javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial")) ++ Jni.defaultSettings

    
  //--- native unix-like settings ----------------------------------------
  
  val UnixBinaryMinorVersion = 0 
  
  lazy val unixNativeSettings: Seq[Setting[_]] = commonNativeSettings ++ Seq(
    flags in Native := Seq("-fPIC", "-O2"),
    linkFlags in Native ++= Seq("-shared", s"-Wl,-soname,libflow.so.${BinaryMajorVersion}"),
    binaryName in Native := s"libflow.so.${BinaryMajorVersion}.${UnixBinaryMinorVersion}",
    nativeSource in Native := baseDirectory.value / "src")

  lazy val nativeLinux = (
    NativeProject("flow-native-linux", file("flow-native") / "unix")
    settings (unixNativeSettings: _*)
    settings (
      includeDirectories in Native += jdkHome.value / "include" / "linux"
    )
    dependsOn(main)
  )
  
  /* stub for native project on a mac, I don't know if this would actually work...
  lazy val nativeMacOSX = (
    NativeProject("flow-native-macosx", file("flow-native") / "unix")
    settings (unixNativeSettings: _*)
    settings (
      includeDirectories in Native += jdkHome.value / "include" / "macosx"
    )
    dependsOn (main)
  )*/
  
}
