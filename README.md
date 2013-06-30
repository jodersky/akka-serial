# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka-IO.

## Motivation
The main reason for yet another serial communication library for the JVM is that all other libraries I tested used blocking IO and/or consumed enormous amounts of CPU while being idle, between 2% and 15%. Flow's main goal is therefore to provide a lightweight library that only does work when communication is required. This reactive concept integrates well with the Akka IO layer therefore making flow an ideal library for extending it.

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertherless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. Currently the native version has only been tested with Linux, however the code should be POSIX compliant and therefore easily portable.

## Usage
(this section will be updated as soon as a maven repository is available)
Clone the repository and run `sbt flow-main/publish-local` to publish the library locally. From there on, you may use the library in any project simply by adding a library dependency to it.

    libraryDependencies += "com.github.jodersky" % "flow" % "1.0-SNAPSHOT"

Examples on flow's usage are located in the flow-samples directory. The examples may be run by switching to the corresponding project in sbt: `project flow-sample-<sample_name>` and typing `run`. Be sure to connect a serial device before running an example.

As flow integrates into the Akka-IO framework, a good resource on its "spirit" is the framework's documentation at http://doc.akka.io/docs/akka/2.2.0-RC2/scala/io.html
        
### Currently supported platforms

| OS    | Architecture | Notes                                                              |
|-------|--------------|--------------------------------------------------------------------|
| Linux | amd64        | A user accessing a serial port needs to be in the 'dialout' group. |


## Build
The building of flow involves two parts
 1. compiling scala sources 9as in a regular project0
 2. compiling native sources that interface with system calls

### Project structure
    flow
    ├── flow-main //main scala/java sources
    ├── flow-native //native sources
    │   ├── include //general headers
    │   └── unix    //source code for unix-like systems
    ├── flow-samples
    ├── project
    └── README.md

## License
Copyright (c) 2013 by Jakob Odersky

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
 derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
