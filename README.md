[![Scaladex](https://index.scala-lang.org/jodersky/akka-serial/akka-serial-core/latest.svg)](https://index.scala-lang.org/jodersky/akka-serial/akka-serial-core)
[![Build Status](https://travis-ci.org/jodersky/akka-serial.svg?branch=master)](https://travis-ci.org/jodersky/akka-serial)
[![Download](https://img.shields.io/maven-central/v/ch.jodersky/akka-serial-native.svg)](http://search.maven.org/#search|ga|1|ch.jodersky%20akka-serial-)

# akka-serial
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications. See the [website](https://jodersky.github.io/akka-serial) for a guide.

## Highlights
- Reactive: only does work when required (no constant polling of ports or blocking IO)
- Integrates seamlessly with Akka
- Portable to POSIX systems
- Watchable ports: react to connection of new device
- Compatible with Reactive Streams

## Native side
Since hardware is involved in serial communication, a Scala-only solution is not possible. Nevertheless, the native code is kept simple and minimalistic with the burden of dealing with threads left to Scala. The code aims to be POSIX compliant and therefore easily portable.

## Directory Structure
```
akka-serial/
├── Documentation         Sources for user documentation as published on the website.
├── core                  Main Scala source files.
├── dev                   Firmware samples for serial devices, to make testing easier.
├── native                C sources used to implement serial communication.
│   └── lib_native        Compiled native libraries that are published in the fat jar.
├── project               Build configuration.
├── samples               Runnable example projects.
├── stream                Stream API, used to connect with Akka streams.
└── sync                  Synchronous, non-Akka-dependent code.
```

*Website source code is in the git branch 'gh-pages'.*

## Build
Detailed documentation on building akka-serial is available on the website (or, equivalently, in [developer.md](Documentation/developer.md)).

Since akka-serial integrates into the Akka-IO framework, a good resource on its general design is the framework's [documentation](http://doc.akka.io/docs/akka/current/scala/io.html).

This project is also an experiment on working with JNI and automating build infrastructure.

## Copying
akka-serial is released under the terms of the 3-clause BSD license. See LICENSE for details.

## Notes
This project used to called "flow". It was renamed to "akka-serial".
