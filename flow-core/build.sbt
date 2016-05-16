import flow.{FlowBuild, Dependencies}

FlowBuild.commonSettings

libraryDependencies += Dependencies.akkaActor

target in javah := (baseDirectory in ThisBuild).value / "flow-native" / "src" / "include"
