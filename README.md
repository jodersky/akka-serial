# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka-IO.

## Motivation
The main reason for yet another serial communication library for the JVM is that all other libraries I tested used blocking IO and/or consumed enormous amounts of CPU while being idle, between 2% and 15%. Flow's main goal is therefore to provide a lightweight library that only does work when communication is required. This reactive concept integrates well with the Akka IO layer therefore making flow an ideal library for extending it.

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Usage
(this section will be updated as soon as a maven repository is available)

Clone the repository and run `sbt flow/publish-local` to publish the library locally. From there on, you may use the library in any project simply by adding a library dependency to it.

    libraryDependencies += "com.github.jodersky" % "flow" % "1.0-SNAPSHOT"

Examples on flow's usage are located in the flow-samples directory. The examples may be run by switching to the corresponding project in sbt: `project flow-samples-<sample_name>` and typing `run`. Be sure to connect a serial device before running an example.

Since flow integrates into the Akka-IO framework, a good resource on its general design is the framework's documentation at http://doc.akka.io/docs/akka/2.2.0/scala/io.html
        
### Currently supported platforms

| OS (tested on)    | Architecture            | Notes                                                                 |
|-------------------|-------------------------|-----------------------------------------------------------------------|
| Linux (3.2.0)     | x86<br>x86_64<br>armv7l | A user accessing a serial port may need to be in the 'dialout' group. |
| Mac OS X (10.6.8) | x86_64                  | Use /dev/cu* device instead of /dev/tty*.                             |

Note: flow may work on older versions of the tested OS kernels.


## Build
A complete build of flow involves two parts

 1. compiling scala sources (as in a regular project)
 2. compiling native sources
 
As any java project, the first part results in a platform independant artifact. However, the second part yields a binary that may only be used on systems resembling the platform for which it was compiled. Nevertheless, to provide multiplatform support, flow produces a "fat executable", a jar containing native binaries compiled for different flavours of operating system and architecture combinations. During runtime, a matching native binary is selected and loaded. To understand how flow can achieve such a mix, it is helpful to look at the project's directory layout.

    flow
    ├── flow-binaries
    ├── flow-main
    ├── flow-native
    │   ├── shared
    │   │   └── include
    │   └── unix
    ├── flow-samples
    └── project

The directories of interest in a build are:

 - flow-main:
 Contains java/scala sources that constitute the main library.
 
 - flow-native:
 Contains native sources used in combination with JNI to support interacting with serial hardware. The directory itself is subdivided into:
   - shared/include:
   general headers describing the native side of the serial API
   - unix:
   source code implementing the native serial API for unix-like operating systems
 
 - flow-binaries:
 Contains binaries produced by various native builds on different platforms. The format of binaries inside this directory is `<os>/<arch>/<libraryname>.<major>.<minor>`, where `<os>` and `<arch>` correspond to the values (without spaces) given by `System.properties` from the JVM.
 

With this structure in mind, building a complete distribution of flow involves (sbt commands are given in code tags):

 1. compiling java/scala sources: `flow/compile`
 This simply compiles any scala and java sources as with any standard sbt project.
 
 2. compiling and linking native sources for the current platform: `flow-native-<os>/native:link`
 This is the most complicated and error-prone step in the build. It involves running javah to generate JNI headers, compiling the native sources for the current platform and linking them.
 Note that for this step to work, a project for the current operating system has to be defined. Take a look at the build file to see how this is done.
 
 3. locally publishing the native binary to include in final jar: `flow-native-<os>/publishNative`
 This copies the compiled binary (for the current platform) to the flow-binaries folder.
 
 4. packaging the final jar: `flow/package`
 This copies the latest major version-compatible shared libraries of flow-binaries to the final jar.
 
The idea behind publishing to an intermediate location is to provide a central collection of binaries that may be created from different systems and included in one final jar (a nice corollary is that anyone can compile native sources on a platform, submit a pull request and have the binary included). As such, if you are only modifying java/scala sources, it is not necessary to compile any native sources and steps 2 and 3 from above may be omitted.
