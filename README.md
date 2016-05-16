[![Build Status](https://travis-ci.org/jodersky/flow.svg?branch=master)](https://travis-ci.org/jodersky/flow)
[![Download](https://api.bintray.com/packages/jodersky/maven/flow-core/images/download.svg)](https://bintray.com/jodersky/maven/flow-core/_latestVersion)

# flow
Serial communication library for Scala, designed to be reactive, lightweight and easily integrable with Akka applications. See the [website](https://jodersky.github.io/flow) for a guide.

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
flow/
├── Documentation         Sources for user documentation as published on the website.
├── dev                   Firmware samples for serial devices, to make testing easier.
├── flow-core             Main Scala source files.
├── flow-native           C sources used to implement serial communication.
│   └── lib_native        Compiled native libraries that are published in flow-native.
├── flow-samples          Runnable example projects.
├── flow-stream           Stream API, used to connect with Akka streams.
└── project               Build configuration.
```

*Website source code is in the git branch 'gh-pages'.*

## Build
Detailed documentation on building flow is available on the website (or, equivalently, in [developer.md](Documentation/developer.md)).

Since flow integrates into the Akka-IO framework, a good resource on its general design is the framework's [documentation](http://doc.akka.io/docs/akka/current/scala/io.html).

This project is also an experiment on working with JNI and automating build infrastructure.

## Copying
flow is released under the terms of the 3-clause BSD license. See LICENSE for details.
