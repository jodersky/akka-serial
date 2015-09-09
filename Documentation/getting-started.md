# Getting Started
Flow uses SBT as build system. To get started, include a dependency to flow-core in your project:

    libraryDependencies += "com.github.jodersky" %% "flow" % "2.2.2"

## Including Native Library
*NOTICE: flow uses native libraries to back serial communication, therefore before you can run any application depending on flow you must include flow's native library! To do so, you have two options.*

### The Easy Way
In case your OS/architecture combination is present in the table below, add a second dependency to your project:

    libraryDependencies += "com.github.jodersky" % "flow-native" % "2.2.2"

| OS                | Architecture                | Notes                                                                           |
|-------------------|-----------------------------|---------------------------------------------------------------------------------|
| Linux             | x86<br/>x86_64<br/>ARM (v7) | A user accessing a serial port will probably need to be in the `dialout` group. |
| Mac OS X          | x86_64                      |                                                                                 |

This will add a jar to your classpath containing native libraries for various platforms. At run time, the correct library for the current platform is selected, extracted and loaded. This solution enables running applications seamlessly, as if they were pure JVM applications.
However, since the JVM does not enable full determination of the current platform (only OS and rough architecture are known), only a couple of platforms can be supported through this solution at the same time.

### Maximum Portability
Do not include the second dependency above. Instead, for every end-user application that relies on flow, manually add the native library for the current platform to the JVM's library path. This can be achieved through various ways, notably:

- Per application:
  Run your program with the command-line option ```-Djava.library.path=".:<folder containing libflow3.so>"```. E.g. ```java -Djava.library.path=".:/home/<folder containing libflow3.so>" -jar your-app.jar```

- System- or user-wide:

	- Copy the native library to a place that is on the default Java library path and run your application normally. Such places usually include `/usr/lib` and `/usr/local/lib`.

	- Use a provided installer (currently Debian archive and Mac .pkg, available in releases)

The native library can either be obtained by building flow (see section Build) or by taking a pre-compiled one, found in releases in the GitHub project. Native libraries need to be of major version 3 to work with this version of flow.


It is recommended that you use the first option only for testing purposes or end-user applications. The second option is recomended for libraries, since it leaves more choice to the end-user.
