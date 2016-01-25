##
## Makefile to create firmware image including arduino sources
##
##


# Arduino variant
#
VARIANT=mega


# CPU model
#
MCU=atmega2560


# CPU Frequency
#
F_CPU=16000000L


# Serial port used for uploading and monitoring
#
SERIAL=/dev/ttyACM0
BAUD=115200


# Name of image to produce (can be arbitrary)
#
TARGET=firmware


# Toolchain and flags
#
CC=avr-gcc
CFLAGS=-Os -Wall -ffunction-sections -fdata-sections -mmcu=$(MCU) -DF_CPU=$(F_CPU) -MMD -DUSB_VID=0x2341 -DUSB_PID=0x8037 -DARDUINO=106

CXX=avr-g++
CXXFLAGS=$(CFLAGS)

LD=avr-gcc
LDFLAGS= -Os -Wl,--gc-sections -mmcu=$(MCU)

AR=avr-ar

AS=avr-as
ASFLAGS=-mmcu=$(MCU)

OC=avr-objcopy
OCFLAGS=

OD=avr-objdump
AVRDUDE=avrdude
AVRDUDEFLAGS= -DV -p $(MCU) -P $(SERIAL) -c stk500v2 -b 115200

AVRSIZE=avr-size

# Derived variables
SOURCEDIRS=\
	src \
	ext/arduino/core \
	$(wildcard ext/arduino/lib/*)

INCLUDES=\
	include \
	ext/arduino/core \
	ext/arduino/variants/$(VARIANT) \
	$(wildcard ext/arduino/lib/*)

SOURCES=\
	$(foreach dir,$(SOURCEDIRS),$(wildcard $(dir)/*.c)) \
	$(foreach dir,$(SOURCEDIRS),$(wildcard $(dir)/*.cpp))
	
OBJECTS=$(addsuffix .o, $(basename $(SOURCES)))

# Rules
all: $(TARGET).hex

$(TARGET).hex: $(TARGET).elf
	$(OC) $(OCFLAGS) -O ihex -R .eeprom $< $@

$(TARGET).elf: $(OBJECTS)
	$(LD) $(LDFLAGS) -o $@ $^
	
# Sources
%.o: %.c
	$(CC) $(CFLAGS) -I$(dir $<) $(addprefix -I, $(INCLUDES)) -o $@ -c $<

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -I$(dir $<) $(addprefix -I, $(INCLUDES)) -o $@ -c $<

# Utility rules
size: $(TARGET).hex
	$(AVRSIZE) --format=avr --mcu=$(MCU) $(TARGET).elf

upload: $(TARGET).hex
	$(AVRDUDE) $(AVRDUDEFLAGS) -U flash:w:$(TARGET).hex:i
	
monitor:
	cu -l $(SERIAL) -s $(BAUD) --parity=none -h

clean:
	@rm -f *.o
	@for dir in $(SOURCEDIRS); do \
		rm -f $$dir/*.o; \
		rm -f $$dir/*.d; \
	done
	@rm -f $(TARGET).hex
	@rm -f $(TARGET).elf
	@rm -f $(GDBINITFILE)
	
.PHONY: clean
