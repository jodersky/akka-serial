#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.68])
AC_INIT(flow, [3.0.1], [jodersky@gmail.com])
AC_CONFIG_MACRO_DIR([m4])
AC_CONFIG_SRCDIR([src/flow_jni.c])

AM_INIT_AUTOMAKE

AC_CONFIG_HEADERS([config.h])

# Checks for programs.
AC_PROG_CC

# Checks for libraries.

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h stddef.h stdlib.h sys/file.h termios.h unistd.h])
#AC_CHECK_HEADERS([jni.h],,AC_MSG_WARN(No jni.h found. Is a JDK installed?))

LT_INIT([disable-static])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_SIZE_T

# Checks for library functions.
AC_FUNC_MALLOC
AC_CHECK_FUNCS([select])

AX_JNI_INCLUDE_DIR
for JNI_INCLUDE_DIR in $JNI_INCLUDE_DIRS
do
    CPPFLAGS="$CPPFLAGS -I$JNI_INCLUDE_DIR"
done

AC_ARG_ENABLE([versioned-lib],
  [AS_HELP_STRING([--disable-versioned-lib], [Disable producing versioned library files.])],
  [:],
  [enable_versioned_lib=yes])

AM_CONDITIONAL([ENABLE_VERSIONED_LIB], [test "$enable_versioned_lib" = "yes"])

EXTRA_CFLAGS=""
EXTRA_LDFLAGS=""

case "$host_os" in
	cygwin*)
		EXTRA_CFLAGS="-D __int64=int64_t"
		EXTRA_LDFLAGS="-no-undefined"
		;;
	*)
		;;
esac

AC_SUBST([EXTRA_CFLAGS])
AC_SUBST([EXTRA_LDFLAGS])

AC_CONFIG_FILES([Makefile])
AC_CONFIG_FILES([src/Makefile])
AC_OUTPUT
