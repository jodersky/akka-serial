enablePlugins(JniNative)

sourceDirectory in nativeCompile := sourceDirectory.value

// package native libraries from lib_native during releases
val isRelease = sys.props("release") == "true"
enableNativeCompilation in Compile := !isRelease
enableNativeCompilation in Test := !isRelease
