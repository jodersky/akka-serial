# Building flow
A complete build of flow involves two parts

 1. compiling scala sources (as in a regular project)
 2. compiling native sources
 
As any java or scala project, the first part results in a platform independant artifact. However, the second part yields a binary that may only be used on systems resembling the platform for which it was compiled. To understand how flow can achieve such a mix, it is helpful to look at the project's directory layout.

    .
    ├── documentation
    ├── flow
    │   └── src
    │       └── main
    │           ├── java
    │           ├── native
    │           └── scala
    ├── flow-pack
    │   └── lib_native
    ├── flow-samples
    └── project

The directories of interest in a build are:

 - flow/src/main/scala and flow/src/main/java:
 Contains java/scala sources that constitute the main library.
 
 - flow/src/main/native:
 Contains native sources used in combination with JNI to support interacting with serial hardware. The directory itself is subdivided into:
   - include: general headers describing the native side of the serial API
   - posix: source code implementing the native serial API for posix-compliant operating systems

With this structure in mind, building a complete distribution of flow involves (sbt commands are given in code tags):

 1. compiling and packaging java/scala sources: `flow/packageBin`
 This simply compiles any scala and java sources as with any standard sbt project and produces a jar ready for being used.
 
 2. compiling and linking native sources for various platforms: `flow/nativeLink`
 This is the most complicated and error-prone step in the build. It involves compiling and cross-compiling the native sources for various platforms and linking them.
 Note that for this step to work, native builds for the current operating system have to be defined. Take a look at the build file to see how this is done (below ```trait Host``` in the file).
 After completing this step, native libraries for the different platforms are available to be copied and included in end-user applications.