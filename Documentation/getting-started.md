# Getting Started
Flow uses SBT as build system. To get started, include a dependency to flow in your project:

	libraryDependencies += "com.github.jodersky" %% "flow" % "2.3.1"

*Note: since version 2.3.0, flow depends on Akka 2.4, therefore requiring a minimum Java Runtime version of 1.8*

Next, you need to include flow's native library that supports communication for serial devices.

## Including Native Library
There are two options to include the native library:

1. Using an easy, pre-packaged dependency, avialble only for certain OSes.

2. Including the library manually for maximum portability.

It is recommended that you use the first option for testing purposes or end-user applications. The second option is recomended for libraries, since it leaves more choice to the end-user.

### The Easy Way
In case your OS/architecture combination is present in the table below, add a second dependency to your project:

    libraryDependencies += "com.github.jodersky" % "flow-native" % "2.3.1"

| OS                | Architecture                | Notes                                                                                                                  |
|-------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------------|
| Linux             | x86<br/>x86_64<br/>ARM (v7) | A user accessing a serial port will usually need to be in the `dialout` group, otherwise permission errors will occur. |
| Mac OS X          | x86_64                      |                                                                                                                        |

This will add a jar to your classpath containing native libraries for various platforms. At run time, the correct library for the current platform is selected, extracted and loaded. This solution enables running applications seamlessly, as if they were pure JVM applications.
However, since the JVM does not enable full determination of the current platform (only OS and rough architecture are known), only a couple of platforms can be supported through this solution at the same time.

### Maximum Portability
First, obtain a copy of the native library, either by building flow (see section [Build](./building)) or by downloading a precompiled version from GitHub releases. Native libraries need to be of major version 3 to work with this version of flow.

Second, for every end-user application that relies on flow, manually add the native library for the current platform to the JVM's library path. This can be achieved through various ways, notably:

- Per application:
  Run your program with the command-line option ```-Djava.library.path=".:<folder containing libflow3.so>"```. E.g. ```java -Djava.library.path=".:/home/<folder containing libflow3.so>" -jar your-app.jar```

- System- or user-wide:

	- Copy the native library to a place that is on the default Java library path and run your application normally. Such places usually include `/usr/lib` and `/usr/local/lib`.

	- Use a provided installer (currently Debian archive and Mac .pkg, available in releases)
