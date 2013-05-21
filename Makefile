TARGET=target
SCALA_VERSION=2.10
CLASSPATH=$(TARGET)/scala-$(SCALA_VERSION)/classes
TARGETDIR=target

all: flow.so

javah: $(CLASSPATH)
	javah -d src/main/c/ -classpath $(CLASSPATH) com.github.jodersky.flow.NativeSerial

flow.o:
	gcc -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -fPIC -c src/main/c/flow.c -o flow.o

flow.so: flow.o
	gcc -shared -Wl,-soname,libflow.so.1 -o libflow.so $< -lc

clean:
	rm -f *.o
	rm -f *.so
