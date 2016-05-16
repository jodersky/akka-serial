import flow.{FlowBuild}

FlowBuild.commonSettings

enablePlugins(JniNative)

sourceDirectory in nativeCompile := sourceDirectory.value

// uncomment below to use library in lib_native instead
//enableNativeCompilation in Compile := false
