# Build native binaries for flow.
# This makefile is intended for linux-based operating systems.

TARGET?=libflow.so
MAJOR_VERSION=2

CC=$(CROSS_COMPILE)gcc
CFLAGS= -O2 -fPIC -Wall
LD=$(CROSS_COMPILE)ld
LDFLAGS=-shared -Wl,-soname,$(TARGET).$(MAJOR_VERSION)
INCLUDES?=../include/ /usr/lib/jvm/java-7-oracle/include/ /usr/lib/jvm/java-7-oracle/include/linux/

OBJECTS=flow.o flow_jni.o

all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC) $(LDFLAGS) -o $@ $^

%.o: %.c
	$(CC) $(CFLAGS) $(addprefix -I, $(INCLUDES)) -o $@ -c $<
	
clean:
	rm -f *.o
	rm -f *.so

.PHONY: clean
