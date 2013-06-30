import sbt._

object Dependencies {

  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.2-M3"
  
  lazy val ioCore = "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"
  lazy val ioFile = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

}