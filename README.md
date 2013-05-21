# flow
Serial communication library for Scala, designed to be reactive, lightweight and easy-to-use.

## Yet another library?
The main reason for yet another serial communication library for the JVM is that all other libraries I tested used blocking IO and ate up enormous amounts of CPU while being idle, between 2% and 15%. Believing that if there is no work to be done, then no work should be done, I went to write flow.

Flow's aim is to provide a reactive serial library easily useable with Scala futures and Akka actors.

## Native side
Since hardware is involved for serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the implementation of non-blocking IO left mainly to Scala. Currently the native version has only been tested with Linux, however the code should be POSIX compliant and therefore easily portable.

## Build & usage
Run sbt in the root directory and switch to project 'flow', by typing 'project flow'. Running 'package' will compile both Scala and native sources and bundle them in the standard jar file. When using the jar in another application, the native library will be automatically extracted and loaded (i.e. no -Djava.library.path or other system parameters have to be set).

To see an example, switch to project 'flow-example', by typing 'project flow-example' and then type 'run'. Don't forget to connect a serial device (such as an arduino) before runinng the example.

The build currently only works on Linux.
