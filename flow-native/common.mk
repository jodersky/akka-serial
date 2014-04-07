# Common settings for flow native build
# =====================================

# Name of the library to produce
#
NAME=flow3


# Library versions
#
# Note that MAJOR should always be set to zero
# since java does not allow loading semantically
# versioned libraries, include the major version
# in the name instead.
#
MAJOR=0# always zero
MINOR=0# backwards-compatible changes
MICRO=0# bugfixes


# Directory where the library will be installed (without /lib)
#
PREFIX?=/usr/lib/jni


# Compiler and linker settings
#
# Set CROSS_COMPILE to a gcc triplet
# when cross-compiling
#
CC=$(CROSS_COMPILE)gcc
LD=$(CROSS_COMPILE)ld
CFLAGS= -O2 -fPIC -Wall
LDFLAGS=


# JNI include directory
#
JNI_INCLUDE?=$(JAVA_HOME)/include


# Include directories
#
INCLUDES=./include


# Objects that will be compiled from respective .c files
#
OBJECTS=flow_jni.o posix/flow.o


