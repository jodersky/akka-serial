import flow.{FlowBuild}

FlowBuild.commonSettings

enablePlugins(JniNative)

sourceDirectory in nativeCompile in Compile := sourceDirectory.value

nativeLibraryPath in Compile := "com/github/jodersky/flow"
