import flow.{FlowBuild, Dependencies}

FlowBuild.commonSettings

libraryDependencies += Dependencies.akkaActor
libraryDependencies += Dependencies.akkaTestKit
libraryDependencies += Dependencies.scalatest

target in javah := (baseDirectory in ThisBuild).value / "flow-native" / "src" / "include"
