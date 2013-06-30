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
      resourceGenerators in Compile <+= (resourceManaged in Compile, link in Native in LocalProject("flow-native-linux")) map { (resDir, binary) =>
      	val file = resDir / "native" / sys.props("os.name").toLowerCase / sys.props("os.arch").toLowerCase / binary.getName
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

  lazy val commonNativeSettings: Seq[Setting[_]] = Seq(
    includeDirectories in Native += file("flow-native") / "include",
    nativeCompile in Native := ((nativeCompile in Native) dependsOn (compile in Compile in main)).value,
    javahClasspath := Seq((classDirectory in Compile in main).value),
    javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial")) ++ Jni.defaultSettings

    
  //--- native unix like settings ----------------------------------------
  
  val UnixBinaryName = "flow"  
  val UnixBinaryMinorVersion = 0 
  
  lazy val unixNativeSettings: Seq[Setting[_]] = commonNativeSettings ++ Seq(
    flags in Native := Seq("-fPIC", "-O2"),
    linkFlags in Native ++= Seq("-shared", s"-Wl,-soname,lib${UnixBinaryName}.so.${BinaryMajorVersion}"),
    binaryName in Native := s"lib${UnixBinaryName}.so.${BinaryMajorVersion}.${UnixBinaryMinorVersion}",
    version := s"${BinaryMajorVersion}.${UnixBinaryMinorVersion}-${sys.props("os.name").toLowerCase}-${sys.props("os.arch").toLowerCase}",
    nativeSource in Native := baseDirectory.value / "src")

  lazy val nativeLinux = (
    NativeProject("flow-native-linux", file("flow-native") / "unix")
    settings (unixNativeSettings: _*)
    settings (
      includeDirectories in Native += jdkHome.value / "include" / "linux"
    )
    dependsOn (main)
  )
  
  
  /*
    Seq(
      libraryDependencies ++= Dependencies.all,
      javahClasses := Seq("com.github.jodersky.flow.internal.NativeSerial"),
      includeDirectories in Native += jdkHome.value / "include" / "linux",
      binaryType in Native := SharedLibrary,
      binaryName in Native := "flow",
      options in Native := Seq("-fPIC", "-O2"),
      linkOptions in Native := Seq("-Wl,-soname,libflow.so.1"),
      
    ) ++ Jni.defaultSettings
    * 
    */
}
