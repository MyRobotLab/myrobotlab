#include "MrlArduino.h"
#include "MrlMsg.h"

MrlArduino::MrlArduino() : Device(DEVICE_TYPE_ARDUINO) {
}

MrlArduino::~MrlArduino() {
	while (pins.size() > 0) {
		delete pins.pop();
	}
}

void MrlArduino::update()
{
		if (pins.size() > 0) {

			MrlMsg msg(PUBLISH_SENSOR_DATA); // the callback id
		    msg.addData(id); // the device Index

			for (int i = 0; i < pins.size(); ++i) {
				Pin* pin = pins.get(i);
				// TODO: moe the analog read outside of thie method and pass it in!
				if (pin->type == ANALOG)	{
					pin->value = analogRead(pin->address);
				} else {
					pin->value = digitalRead(pin->address);
				}

				// loading both analog & digital data
				msg.addData(pin->address);
				msg.addData16(pin->value);

			}

			// sending back the message
			msg.sendMsg();
		}
}
