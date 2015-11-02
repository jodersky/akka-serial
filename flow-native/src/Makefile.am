lib_LTLIBRARIES = libflow3.la

libflow3_la_SOURCES = flow_jni.c platform/posix/flow.c flow.h com_github_jodersky_flow_internal_NativeSerial.h

if ENABLE_VERSIONED_LIB
libflow3_la_LDFLAGS = -version-info 0:1:0 $(EXTRA_LDFLAGS)
else 
libflow3_la_LDFLAGS = -avoid-version $(EXTRA_LDFLAGS)
endif

AM_CFLAGS = $(EXTRA_CFLAGS)
