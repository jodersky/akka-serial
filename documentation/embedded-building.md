# Locally building on an embedded device
Setting up cross-compilation on your host machine can be quite a daunting task, it involves installing and maybe even compiling other compilers and toolchains. The advantage of doing so is that you can easily build flow for different platforms at once and for systems that may not have the required resources to run sbt. However, if you are only targeting one specific platform that has the ability to run a C compiler and linker you can still build flow without the use of sbt.

## Requirements
- a target platform that has enough resources for compiling native programs
- a host platform that can run SBT

## Overview of required steps
1. Compile native sources on target platform to a library
2. Compile scala/java sources on host
3. Use the compiled library with the scala/java application

## Detailed procedure
This section details the procedure for linux-based target platforms.

1. Compilation of native sources [on the target platform]

    a. Find where the jni include directory is. If you are using a recent oracle java distribution this would typically be /usr/lib/jvm/java-7-oracle/include/ and /usr/lib/jvm/java-7-oracle/include/linux

    b. cd to flow/src/main/native

    c. Compile: ```gcc -O2 -fPIC -I./include/ -I/usr/lib/jvm/java-7-oracle/include/ -I/usr/lib/jvm/java-7-oracle/include/linux -o flow.o -c posix/flow.c``` (replace the -I options with the paths found in a)

    d. Link: ```gcc -shared -Wl,-soname,libflow.so.2 -o libflow.so flow.o```

    e. That's all for the native side, you have a shared library backing flow. Copy libflow.so to any location you wish (see below).

2. Compilation of scala/java sources [on the host platform]

    a. ```sbt compile```

    b. ```sbt publishLocal``` or ```sbt package``` or however you wish to publish flow

    c. That's all regarding the scala/java side, you have a build of flow that is ready to be included in your projects.

3. Putting it all together. 

    In your scala/java application, treat flow as if it were a pure scala/java library and build your application with flow as a usual dependency. However, when running your application or any other application that relies on it, the native library must be included in java's library path. To do so, you have several options:

    - Per application:
        - Run java with the command-line option -Djava.library.path=".:/home/<folder containing libflow.so>". E.g. ```java -Djava.library.path=".:/home/<folder containing libflow.so>" -jar your-app.jar```
        - Run your program by prepending LD_LIBRARY_PATH=<folder containing libflow.so> to the command. E.g ```LD_LIBRARY_PATH=<folder containing libflow.so> java -jar your-app.jar```

    - System- or user-wide:
        - Copy libflow.so to a place that is on the default java library path and run your application normally. Such places usually include /usr/lib and /usr/local/lib
