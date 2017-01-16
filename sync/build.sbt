import flow.Dependencies

libraryDependencies += Dependencies.scalatest % "test"

target in javah := (baseDirectory in ThisBuild).value / "native" / "src" / "include"
