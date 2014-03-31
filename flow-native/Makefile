# Build native binaries for flow.
# ===============================
# This makefile is intended for linux based systems.

# Include common settings
include common.mk

# Name of library to build
LIB=lib$(NAME).so

# soname of library
SONAME=$(LIB).$(MAJOR)

# Actual file name of library
TARGET=$(SONAME).$(MINOR).$(MICRO)

# Linux-specific flags
LDFLAGS+=-shared -Wl,-soname,$(SONAME)
INCLUDES+=$(JAVA_HOME)/include/ $(JAVA_HOME)/include/linux/

# All final products that should be installed
PRODUCTS=$(TARGET) $(SONAME) $(LIB)


# Build library and all appropriate links
all: $(PRODUCTS)

# Build actual library
$(TARGET): $(OBJECTS)
	$(CC) $(LDFLAGS) -o $@ $^

# Create soname link
$(SONAME): $(TARGET)
	ln -s $< $@

# Create dev link
$(LIB): $(SONAME)
	ln -s $< $@

# Compile objects
%.o: %.c
	$(CC) $(CFLAGS) $(addprefix -I, $(INCLUDES)) -o $@ -c $<

# Install resulting artifacts
install: all
	@for product in $(PRODUCTS); do cp -P -t $(DESTDIR)/$(PREFIX)/lib $${product}; done

# Uninstall resulting artifacts
uninstall:
	@for product in $(PRODUCTS); do rm $(DESTDIR)/$(PREFIX)/lib/$${product}; done

# Clean build
clean:
	rm -f posix/*.o
	rm -f posix/*.so
	rm -f *.o
	$(foreach product, $(PRODUCTS), rm -f $(product);)

.PHONY: clean install uninstall