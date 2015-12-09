enablePlugins(JniJvm)

//there are also java sources in this project
compileOrder in Compile := CompileOrder.Mixed

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"

target in (Compile, javah) := (baseDirectory in ThisBuild).value / "flow-native" / "src" / "src"

name := "flow"
