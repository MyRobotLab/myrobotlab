#ifndef MrlAnalogPinArray_h
#define MrlAnalogPinArray_h

#include "Device.h"
#include "LinkedList.h"
#include "MrlMsg.h"

/**
 * MrlArduino device is a device which manages reads from pins
 * It is the first device added to MrlComm,
 * it also reduces the complexity of analogRead, digitalRead, to simply "read"
 * but in order to do so it must know the "type" of Arduino its running on
 */
class MrlArduino: public Device {
public:
	LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
	MrlArduino() :
			Device(SENSOR_TYPE_ARDUINO) {
	}
	~MrlArduino() {
		while (pins.size() > 0) {
			delete pins.pop();
		}
	}

	void update() {
		if (pins.size() > 0) {

			MrlMsg msg(PUBLISH_SENSOR_DATA);
		    msg.addData(id); // device Index
		    msg.addData(ioCmd[3]);

			for (int i = 0; i < pins.size(); ++i) {
				Pin* pin = pins.get(i);
				// TODO: moe the analog read outside of thie method and pass it in!
				if (pin->type == ANALOG)	{
					pin->value = analogRead(pin->address);
				} else {
					pin->value = digitalRead(pin->address);
				}

				msg.addData(pin->address);
				msg.addData16(pin->value);

			}

			msg.sendMsg();
		}
	}
};

#endif
