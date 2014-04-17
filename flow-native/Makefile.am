AUTOMAKE_OPTIONS = foreign

AM_CPPFLAGS = -I$(top_builddir)/include

lib_LTLIBRARIES = libflow.la
libflow_la_SOURCES = flow/flow_jni.h platform/posix/flow.c include/com_github_jodersky_flow_internal_NativeSerial include/flow.h

libflow_la_LDFLAGS = -release 3