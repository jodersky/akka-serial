#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.68])
AC_INIT([flow], [3.0.0], [jodersky@gmail.com])
AC_CONFIG_SRCDIR([include/com_github_jodersky_flow_internal_NativeSerial.h])
AC_CONFIG_HEADERS([config.h])

# Checks for programs.
AC_PROG_CC

# Checks for libraries.

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h stddef.h stdlib.h sys/file.h termios.h unistd.h])
AC_CHECK_HEADERS([jni.h],,AC_MSG_WARN(No jni.h found. Is a JDK installed?))

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_SIZE_T

# Checks for library functions.
AC_FUNC_MALLOC
AC_CHECK_FUNCS([select])

LT_INIT
AM_INIT_AUTOMAKE

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
