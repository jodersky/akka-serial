# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications.

## Motivation
The main reason for yet another serial communication library for the JVM is that all other libraries tested used blocking IO and/or consumed enormous amounts of CPU while being idle. Flow's main goal is therefore to provide a lightweight library that only does work when communication is required. This reactive concept integrates well with the Akka IO layer therefore making flow an ideal library for extending it.

## Basic usage
For a short guide on how to use flow see the file [documentation/basics.md](documentation/basics.md).

Flow is built and its examples run with SBT. To get started, include a dependency to flow in your project:

    libraryDependencies += "com.github.jodersky" %% "flow" % "2.0.0"

ATTENTION: flow uses native libraries to back serial communication, therefore before you can run any application depending on flow you must include flow's native library in the JVM library path. Check out section 'build' on how this may be done.

## Examples
Examples on flow's usage are located in the flow-samples directory. The examples may be run by switching to the corresponding project in sbt: `project flow-samples-<sample_name>` and typing `run`. Be sure to connect a serial device before running an example.

Since flow integrates into the Akka-IO framework, a good resource on its general design is the framework's documentation at http://doc.akka.io/docs/akka/2.2.0/scala/io.html

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Build
See detailed documentation in [documentation/building.md](documentation/building.md) on how to build and install flow.

## License
flow is released under the terms of the 3-clause BSD license. See LICENSE for details.
