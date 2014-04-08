# Build native binaries for flow.
# ===============================
# This makefile is intended for linux based systems.

# Include common settings
include common.mk

# Canonical name of library
CNAME=lib$(NAME).so

# Java library link, used to enable loading of
# semanticaly versioned librraies from java
JAVALIB=lib$(NAME)$(MAJOR).so

# soname of library
SONAME=$(CNAME).$(MAJOR)

# Actual file name of library
TARGET=$(SONAME).$(MINOR).$(MICRO)

# Linux-specific flags
LDFLAGS+=-shared -Wl,-soname,$(SONAME)
INCLUDES+=$(JNI_INCLUDE)

# All final products that should be installed
PRODUCTS=$(TARGET) $(SONAME) $(JAVALIB)

# Build library and all appropriate links
all: $(PRODUCTS)

# Build actual library
$(TARGET): $(OBJECTS)
	$(CC) $(LDFLAGS) -o $@ $^

# Create soname link
$(SONAME): $(TARGET)
	ln -s $< $@

# Create dev link
$(JAVALIB): $(SONAME)
	ln -s $< $@

# Compile objects
%.o: %.c
	$(CC) $(CFLAGS) $(addprefix -I, $(INCLUDES)) -o $@ -c $<

# Install resulting artifacts
install: all
	@for product in $(PRODUCTS); do cp -P -t $(DESTDIR)/$(PREFIX) $${product}; done

# Uninstall resulting artifacts
uninstall:
	@for product in $(PRODUCTS); do rm $(DESTDIR)/$(PREFIX)/$${product}; done

# Clean build
clean:
	rm -f posix/*.o
	rm -f posix/*.so
	rm -f *.o
	$(foreach product, $(PRODUCTS), rm -f $(product);)

.PHONY: clean install uninstall