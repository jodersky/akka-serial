# Build native binaries for flow.
# ===============================

# name of the library to produce
NAME=flow
MAJOR=3
MINOR=0
MICRO=0

SONAME=lib$(NAME).so.$(MAJOR)
TARGET=$(SONAME).$(MINOR).$(MICRO)

JAVA_HOME=/usr/lib/jvm/java-7-oracle

# where the library should be installed
PREFIX?=usr/lib

# compiler and linker settings
CC=$(CROSS_COMPILE)gcc
LD=$(CROSS_COMPILE)ld
CFLAGS= -O2 -fPIC -Wall

# since java connot easily load a library with a specific
# major version, the vesion is included in the library's name

LDFLAGS+=-shared -Wl,-soname,$(SONAME)
INCLUDES=./include/ $(JAVA_HOME)/include/ $(JAVA_HOME)/include/linux/

OBJECTS=flow_jni.o posix/flow.o

# rules
all: $(TARGET) $(SONAME)

$(TARGET): $(OBJECTS)
	$(CC) $(LDFLAGS) -o $@ $^
	
$(SONAME): $(TARGET)
	ln -s $(TARGET) $(SONAME)

%.o: %.c
	$(CC) $(CFLAGS) $(addprefix -I, $(INCLUDES)) -o $@ -c $<

install:
	cp -P -t $(DESTDIR)/$(PREFIX)/ $(TARGET) $(SONAME)

uninstall:
	rm $(DESTDIR)/$(PREFIX)/$(TARGET)
	rm $(DESTDIR)/$(PREFIX)/$(SONAME)
	
clean:
	rm -f posix/*.o
	rm -f posix/*.so
	rm -f $(TARGET)
	rm -f $(SONAME)

.PHONY: clean install
