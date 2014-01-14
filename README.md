# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications.

## Motivation
The main reason for yet another serial communication library for the JVM is that all other libraries I tested used blocking IO and/or consumed enormous amounts of CPU while being idle, between 2% and 15%. Flow's main goal is therefore to provide a lightweight library that only does work when communication is required. Furthermore, this reactive concept integrates well with the Akka IO layer therefore making flow an ideal library for extending it.

## Basic usage
For a short guide on how to use flow see the file "documentation/basics.md", accessible on github [here](https://github.com/jodersky/flow/blob/master/documentation/basics.md).

## Currently supported platforms

| OS (tested on)    | Architecture            | Notes                                                                 |
|-------------------|-------------------------|-----------------------------------------------------------------------|
| Linux (3.2.0)     | x86<br>x86_64<br>armv7  | A user accessing a serial port may need to be in the 'dialout' group. |
| Mac OS X (10.6.8) | x86_64                  | Use /dev/cu* device instead of /dev/tty*.                             |

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Build
The build is currently being restructured for less cumbersome native compilation and easier cross-compilation (e.g. for targeting embedded platforms like the Raspberry Pi).
Current status: building works, however the production of fat jars containing native libraries is not yet supported.

See documentation/embedded-building.md for a description on how to build the native part of flow on memory restricted devices, requiring only a C compiler and linker.

## License
flow is released under the terms of the 3-clause BSD license. See LICENSE for details.