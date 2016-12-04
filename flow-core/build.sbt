import flow.Dependencies

libraryDependencies += Dependencies.akkaActor
libraryDependencies += Dependencies.akkaTestKit % "test"
libraryDependencies += Dependencies.scalatest % "test"

target in javah := (baseDirectory in ThisBuild).value / "flow-native" / "src" / "include"
