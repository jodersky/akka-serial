lib_LTLIBRARIES = libflow3.la

libflow3_la_SOURCES = flow_jni.c platform/posix/flow.c flow.h com_github_jodersky_flow_internal_NativeSerial.h

if ENABLE_VERSIONED_LIB
libflow3_la_LDFLAGS = -version-info 0:1:0
else 
libflow3_la_LDFLAGS = -avoid-version
endif