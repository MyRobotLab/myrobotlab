#ifndef MrlArduino_h
#define MrlArduino_h

#define ANALOG			    1
#define DIGITAL			    2

#include "Device.h"
#include "LinkedList.h"
#include "Pin.h"

/**
 * MrlArduino device is a device which manages reads from pins
 * It is the first device added to MrlComm,
 * it also reduces the complexity of analogRead, digitalRead, to simply "read"
 * but in order to do so it must know the "type" of Arduino its running on
 */
class MrlArduino: public Device {
public:
	LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0

	MrlArduino();
	~MrlArduino();

	void update();
};

#endif
