#include "Arduino.h"

#define BAUD_RATE 115200

//called once
void setup() {
	Serial.begin(BAUD_RATE);
}

//called repeatedly
void loop() {
	delay(10);
}

void serialEvent() {
	char buffer[64];
	uint8_t idx = 0;

	while (Serial.available()) {
		if (idx == 62) {
			Serial.println("Input too long");
			return;
		}

		char in = (char) Serial.read();
		buffer[idx] = in;
		idx += 1;
	}
	buffer[idx+1] = '\0';
	Serial.print("echo: ");
	Serial.write(buffer);
}
