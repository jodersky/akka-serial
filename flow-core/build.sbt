import flow.{FlowBuild, Dependencies}

FlowBuild.commonSettings

libraryDependencies += Dependencies.akkaActor

//there are also java sources in this project
compileOrder in Compile := CompileOrder.Mixed

enablePlugins(JniLoading)

target in javah in Compile := (baseDirectory in ThisBuild).value / "flow-native" / "src" / "include"
