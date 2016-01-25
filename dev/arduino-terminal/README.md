# Arduino Echo Terminal

Sample code for an echo terminal. The program listens on UART0, baud rate 115200 and echos any data back to the sender, prefixed with "echo: ".

By default, the project is configured for the Arduino Mega series, please see the Makefile for any customization.

## Dependencies

- avr-gcc toolchain
- avrdude

Note: Arduino source files included, no need to install the Arduino IDE.

## Directory structure

- ext: external sources and headers (i.e. arduino files)
- src: project-specific sources
- include: project-specific header files

## Main targets
- all: compile, link and create a firmware image (intel hex format)
- upload: call `avrdude` to upload firmware to device
- monitor: call `cu` to connect to device through serial interface
- clean: delete any temporary files

## Copying
This project includes Arduino source files in `ext/arduino`.

Arduino is an open source project, supported by many.

The Arduino team is composed of Massimo Banzi, David Cuartielles, Tom Igoe and David A. Mellis.

Arduino uses GNU avr-gcc toolchain, GCC ARM Embedded toolchain, avr-libc, avrdude, bossac, openOCD and code from Processing and Wiring.

Icon and about image designed by ToDo

Released under the GNU General Public License.
