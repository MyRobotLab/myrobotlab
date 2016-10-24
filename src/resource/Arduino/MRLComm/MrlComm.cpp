#include "MrlComm.h"

MrlComm::MrlComm() {
	softReset();
	byteCount = 0;
	mrlCmd[0] = new MrlCmd(MRL_IO_SERIAL_0);
	for (unsigned int i = 1; i < (sizeof(mrlCmd) / sizeof(MrlCmd*)); i++) {
		mrlCmd[i] = NULL;
	}

}

MrlComm::~MrlComm() {
	for (unsigned int i = 0; i < (sizeof(mrlCmd) / sizeof(MrlCmd*)); i++) {
		if (mrlCmd[i] != NULL) {
			delete mrlCmd[i];
		}
	}
}
/***********************************************************************
 * UTILITY METHODS BEGIN
 */
void MrlComm::softReset() {
	while (deviceList.size() > 0) {
		delete deviceList.pop();
	}
  while (pinList.size() > 0) {
    delete pinList.pop();
  }
	//resetting var to default
	loopCount = 0;
	publishBoardStatusModulus = 10000;
	enableBoardStatus = false;
	Device::nextDeviceId = 1; // device 0 is Arduino
	debug = false;
	for (unsigned int i = 1; i < (sizeof(mrlCmd) / sizeof(MrlCmd*)); i++) {
		if (mrlCmd[i] != NULL) {
			mrlCmd[i]->end();
			delete mrlCmd[i];
			mrlCmd[i] = NULL;
		}
	}
	heartbeat = false;
	heartbeatEnabled = false;
	lastHeartbeatUpdate = 0;
	for (unsigned int i = 0; i < MAX_MSG_SIZE; i++) {
	  customMsg[i] = 0;
	}
	customMsgSize = 0;
}

/***********************************************************************
 * PUBLISH_BOARD_STATUS
 * This function updates the average time it took to run the main loop
 * and reports it back with a publishBoardStatus MRLComm message
 *
 * TODO: avgTiming could be 0 if loadTimingModule = 0 ?!
 *
 * MAGIC_NUMBER|7|[loadTime long0,1,2,3]|[freeMemory int0,1]
 */
void MrlComm::publishBoardStatus() {

	// protect against a divide by zero in the division.
	if (publishBoardStatusModulus == 0) {
		publishBoardStatusModulus = 10000;
	}

	unsigned int avgTiming = 0;
	unsigned long now = micros();

	avgTiming = (now - lastMicros) / publishBoardStatusModulus;

	// report board status
	if (enableBoardStatus && (loopCount % publishBoardStatusModulus == 0)) {

		// send the average loop timing.
		MrlMsg msg(PUBLISH_BOARD_STATUS);
		msg.addData16(avgTiming);
		msg.addData16(getFreeRam());
		msg.addData16(deviceList.size());
		msg.sendMsg();
	}
	// update the timestamp of this update.
	lastMicros = now;
}

int MrlComm::getFreeRam() {
	// KW: In the future the arduino might have more than an 32/64k of ram. an int might not be enough here to return.
	extern int __heap_start, *__brkval;
	int v;
	return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
}

/***********************************************************************
 * PUBLISH DEVICES BEGIN
 *
 * All serial IO should happen here to publish a MRLComm message.
 * TODO: move all serial IO into a controlled place this this below...
 * TODO: create MRLCommMessage class that can just send itself!
 *
 */
/**
 * Publish the MRLComm message
 * MAGIC_NUMBER|2|MRLCOMM_VERSION
 */
void MrlComm::publishVersion() {
	MrlMsg msg(PUBLISH_VERSION);
	msg.addData(MRLCOMM_VERSION);
	msg.sendMsg();
}
/**
 * publishBoardInfo()
 * MAGIC_NUMBER|2|PUBLISH_BOARD_INFO|BOARD
 * return the board type (mega/uno) that can use in javaland for the pin layout
 */
void MrlComm::publishBoardInfo() {
	MrlMsg msg(PUBLISH_BOARD_INFO);
	msg.addData(BOARD);
	msg.sendMsg();
}

/**
 * Publish the acknowledgement of the command received and processed.
 * MAGIC_NUMBER|2|PUBLISH_MESSAGE_ACK|FUNCTION
 */
void MrlComm::publishCommandAck(int function) {
	MrlMsg msg(PUBLISH_MESSAGE_ACK);
	// the function that we're ack-ing
	msg.addData(function);
	msg.sendMsg();
}
/**
 * PUBLISH_ATTACHED_DEVICE
 * MSG STRUCTURE
 * PUBLISH_ATTACHED_DEVICE | NEW_DEVICE_INDEX | NAME_STR_SIZE | NAME
 *
 */
void MrlComm::publishAttachedDevice(int id, int nameSize, unsigned char* name) {
	MrlMsg msg(PUBLISH_ATTACHED_DEVICE, id);
	msg.addData(name, nameSize, true);
	msg.sendMsg();
}

/***********************************************************************
 * SERIAL METHODS BEGIN
 */
void MrlComm::readCommand() {
	for (unsigned int i = 0; i < (sizeof(mrlCmd) / sizeof(MrlCmd*)); i++) {
		if (mrlCmd[i] != NULL) {
			if (mrlCmd[i]->readCommand()) {
				processCommand(i + 1);
			}
		}
	}
}

// This function will switch the current command and call
// the associated function with the command
/**
 * processCommand() - once the main loop has read an mrlcomm message from the 
 * serial port, this method will be called.
 */
void MrlComm::processCommand(int ioType) {
	unsigned char* ioCmd = mrlCmd[ioType - 1]->getIoCmd();
	if (ioType != MRL_IO_SERIAL_0) {
		MrlMsg msg = MrlMsg(MSG_ROUTE);
		msg.addData(ioType);
		msg.addData(ioCmd, mrlCmd[ioType - 1]->getMsgSize());
		msg.sendMsg();
		//  MrlMsg::publishDebug("not from Serial, ioType" + String(ioType));
		return;
	}
	// FIXME - all case X: should have scope operator { } !
	// MrlMsg::publishDebug("not from Serial:" + String(ioCmd[0]));
	switch (ioCmd[0]) {
	// === system pass through begin ===
	case DIGITAL_WRITE:
		digitalWrite(ioCmd[1], ioCmd[2]);
		break;
	case ANALOG_WRITE: {
		analogWrite(ioCmd[1], ioCmd[2]);
		break;
	}
	case PIN_MODE: {
		pinMode(ioCmd[1], ioCmd[2]);
		break;
	}
	case SERVO_ATTACH: {
		int pin = ioCmd[2];
		if (debug)
			MrlMsg::publishDebug("SERVO_ATTACH " + String(pin));
		MrlServo* servo = (MrlServo*) getDevice(ioCmd[1]);
		servo->attach(pin);
		if (debug)
			MrlMsg::publishDebug(F("SERVO_ATTACHED"));
		break;
	}
	case SERVO_SWEEP_START:
		//startSweep(min,max,step)
		((MrlServo*) getDevice(ioCmd[1]))->startSweep(ioCmd[2], ioCmd[3],
				ioCmd[4]);
		break;
	case SERVO_SWEEP_STOP:
		((MrlServo*) getDevice(ioCmd[1]))->stopSweep();
		break;
	case SERVO_WRITE:
		((MrlServo*) getDevice(ioCmd[1]))->servoWrite(ioCmd[2]);
		break;
	case SERVO_WRITE_MICROSECONDS:
		((MrlServo*) getDevice(ioCmd[1]))->servoWriteMicroseconds(MrlMsg::toInt(ioCmd, 2));
		break;
	case SERVO_DETACH: {
		if (debug)
			MrlMsg::publishDebug("SERVO_DETACH " + String(ioCmd[1]));
		((MrlServo*) getDevice(ioCmd[1]))->detach();
		if (debug)
			MrlMsg::publishDebug("SERVO_DETACHED");
		break;
	}
	case ENABLE_BOARD_STATUS:
		enableBoardStatus = true;
		publishBoardStatusModulus = (unsigned int) MrlMsg::toInt(ioCmd, 1);
		if (debug)
			MrlMsg::publishDebug(
					"modulus is " + String(publishBoardStatusModulus));
		break;

		// ENABLE_PIN_EVENTS | ADDRESS | PIN TYPE 0 = DIGITAL | 1 = ANALOG
	case ENABLE_PIN: {
		int address = ioCmd[1];
		int type = ioCmd[2];
		int rate = MrlMsg::toInt(ioCmd, 3);
		// don't add it twice
		for (int i = 0; i < pinList.size(); ++i) {
			Pin* pin = pinList.get(i);
			if (pin->address == address) {
				// TODO already exists error?
				break;
			}
		}

		if (type == DIGITAL) {
			pinMode(address, INPUT);
		}
		Pin* p = new Pin(address, type, rate);
		p->lastUpdate = 0;
		pinList.add(p);
		break;
	}
	case DISABLE_PIN: {
		int address = ioCmd[1];
    ListNode<Pin*>* node = pinList.getRoot();
    int index = 0;
    while (node != NULL) {
      if (node->data->address == address) {
        delete node->data;
        pinList.remove(index);
        break;
      }
      node = node->next;
      index++;
    }
		break;
	}

	case DISABLE_PINS: {
    while (pinList.size() > 0) {
      delete pinList.pop();
    }
		break;
	}
	case DISABLE_BOARD_STATUS:
		enableBoardStatus = false;
		break;
	case SET_PWMFREQUENCY:
		setPWMFrequency(ioCmd[1], ioCmd[2]);
		break;
	case PULSE:
		//((MrlPulse*)getDevice(ioCmd[1]))->pulse(ioCmd);
		break;
	case PULSE_STOP:
		//((MrlPulse*)getDevice(ioCmd[1]))->pulseStop();
		break;
	case SET_TRIGGER:
		//setTrigger();
		break;
	case SET_DEBOUNCE:
		//setDebounce();
		break;
	case SET_DIGITAL_TRIGGER_ONLY:
		//setDigitalTriggerOnly();
		break;
	case SET_SERIAL_RATE:
		setSerialRate();
		break;
	case GET_VERSION:
		publishVersion();
		break;
	case SET_SAMPLE_RATE:
		//setSampleRate();
		break;
	case SOFT_RESET:
		softReset();
		break;
	case SENSOR_POLLING_START:
		//sensorPollingStart();
		break;
	case DEVICE_ATTACH:
		deviceAttach(ioCmd);
		break;
	case DEVICE_DETACH:
		deviceDetach(ioCmd[1]);
		break;
	case SENSOR_POLLING_STOP:
		//sensorPollingStop();
		break;
		// Start of i2c read and writes
	case I2C_READ:
		((MrlI2CBus*) getDevice(ioCmd[1]))->i2cRead(ioCmd);
		break;
	case I2C_WRITE:
		((MrlI2CBus*) getDevice(ioCmd[1]))->i2cWrite(ioCmd);
		break;
	case I2C_WRITE_READ:
		((MrlI2CBus*) getDevice(ioCmd[1]))->i2cWriteRead(ioCmd);
		break;
	case SET_DEBUG:
		debug = ioCmd[1];
		if (debug) {
			MrlMsg::publishDebug(F("Debug logging enabled."));
		}
		break;
	case PUBLISH_BOARD_INFO:
		publishBoardInfo();
		break;
	case NEO_PIXEL_WRITE_MATRIX:
		((MrlNeopixel*) getDevice(ioCmd[1]))->neopixelWriteMatrix(ioCmd);
		break;
	case NEO_PIXEL_SET_ANIMATION:
		((MrlNeopixel*) getDevice(ioCmd[1]))->setAnimation(ioCmd+2);
		break;
	case CONTROLLER_ATTACH:
		mrlCmd[ioCmd[1] - 1] = new MrlCmd(ioCmd[1]);
		break;
	case MSG_ROUTE: {
		MrlMsg msg(ioCmd[2]);
		msg.addData(ioCmd + 3, mrlCmd[ioType - 1]->getMsgSize() - 3);
		msg.begin(ioCmd[1], 115200);
		msg.sendMsg();
		break;
	}
	case SERVO_SET_MAX_VELOCITY: {
		((MrlServo*) getDevice(ioCmd[1]))->setMaxVelocity(MrlMsg::toInt(ioCmd,3));
		break;
	}
	case SERVO_SET_VELOCITY: {
		((MrlServo*) getDevice(ioCmd[1]))->setVelocity(MrlMsg::toInt(ioCmd,3));
		break;
	}
	case HEARTBEAT: {
		heartbeatEnabled = true;
		break;
	}
	case CUSTOM_MSG: {
	  for (byte i = 0; i < ioCmd[1] && customMsgSize < 64; i++) {
	    customMsg[customMsgSize] = ioCmd[i+2];
	    customMsgSize++;
	  }
	  break;
	}
	default:
		MrlMsg::publishError(ERROR_UNKOWN_CMD);
		break;
	} // end switch
	  // ack that we got a command (should we ack it first? or after we process the command?)
	heartbeat = true;
	lastHeartbeatUpdate = millis();
	publishCommandAck(ioCmd[0]);
	// reset command buffer to be ready to receive the next command.
	// KW: we should only need to set the byteCount back to zero. clearing this array is just for safety sake i guess?
	// GR: yup
	//memset(ioCmd, 0, sizeof(ioCmd));
	//byteCount = 0;
} // process Command
/***********************************************************************
 * CONTROL METHODS BEGIN
 * These methods map one to one for each MRLComm command that comes in.
 * 
 * TODO - add text api
 */

// SET_PWMFREQUENCY
void MrlComm::setPWMFrequency(int address, int prescalar) {
	// FIXME - different boards have different timers
	// sets frequency of pwm of analog
	// FIXME - us ifdef appropriate uC which
	// support these clocks TCCR0B
	int clearBits = 0x07;
	if (address == 0x25) {
		TCCR0B &= ~clearBits;
		TCCR0B |= prescalar;
	} else if (address == 0x2E) {
		TCCR1B &= ~clearBits;
		TCCR1B |= prescalar;
	} else if (address == 0xA1) {
		TCCR2B &= ~clearBits;
		TCCR2B |= prescalar;
	}
}

// SET_SERIAL_RATE
void MrlComm::setSerialRate() {
	//mrlCmd->end();
	//mrlCmd->begin(MRL_IO_SERIAL_0,mrlCmd->getIoCmd(1));
}

/**********************************************************************
 * ATTACH DEVICES BEGIN
 *
 *<pre>
 *
 * MSG STRUCTURE
 *                    |<-- ioCmd starts here                                        |<-- config starts here
 * MAGIC_NUMBER|LENGTH|ATTACH_DEVICE|DEVICE_TYPE|NAME_SIZE|NAME .... (N)|CONFIG_SIZE|DATA0|DATA1 ...|DATA(N)
 *
 * ATTACH_DEVICE - this method id
 * DEVICE_TYPE - the mrlcomm device type we are attaching
 * NAME_SIZE - the size of the name of the service of the device we are attaching
 * NAME .... (N) - the name data
 * CONFIG_SIZE - the size of the folloing config
 * DATA0|DATA1 ...|DATA(N) - config data
 *
 *</pre>
 *
 * Device types are defined in org.myrobotlab.service.interface.Device
 * TODO crud Device operations create remove (update not needed?) delete
 * TODO probably need getDeviceId to decode id from Arduino.java - because if its
 * implemented as a ptr it will be 4 bytes - if it is a generics id
 * it could be implemented with 1 byte
 */
void MrlComm::deviceAttach(unsigned char* ioCmd) {
	// TOOD:KW check free memory to see if we can attach a new device. o/w return an error!
	// we're creating a new device. auto increment it
	// TODO: consider what happens if we overflow on this auto-increment. (very unlikely. but possible)
	// we want to echo back the name
	// and send the config in a nice neat package to
	// the attach method which creates the device
	//unsigned char* ioCmd = mrlCmd->getIoCmd();
	int nameSize = ioCmd[2];

	// get config size
	int configSizePos = 3 + nameSize;
	int configSize = ioCmd[configSizePos];
	int configPos = configSizePos + 1;
	config = ioCmd + configPos;
	// MAKE NOTE: I've chosen to have config & configPos globals
	// this is primarily to avoid the re-allocation/de-allocation of the config buffer
	// but part of me thinks it should be a local var passed into the function to avoid
	// the dangers of global var ... fortunately Arduino is single threaded
	// It also makes sense to pass in config on the constructor of a new device
	// based on device type - "you inflate the correct device with the correct config"
	// but I went on the side of globals & hopefully avoiding more memory management and fragmentation
	// CAL: change config to a pointer in ioCmd (save some memory) so config[0] = ioCmd[configPos]

	int type = ioCmd[1];
	Device* devicePtr = 0;
	// KW: remove this switch statement by making "attach(int[]) a virtual method on the device base class.
	// perhaps a factory to produce the devicePtr based on the deviceType..
	// currently the attach logic is embeded in the constructors ..  maybe we can make that a more official
	// lifecycle for the devices..
	// check out the make_stooge method on https://sourcemaking.com/design_patterns/factory_method/cpp/1
	// This is really how we should do this.  (methinks)
	// Cal: the make_stooge method is certainly more C++ like, but essentially do the same thing as we do,
	// it just move this big switch to another place

	// GR: I agree ..  "attach" should be a universal concept of devices, yet it does not need to be implmented
	// in the constructor .. so I'm for making a virtualized attach, but just like Java-Land the attach
	// needs to have size sent in with the config since it can be variable array
	// e.g.  attach(int[] config, configSize)

	switch (type) {
	case DEVICE_TYPE_ARDUINO: {
		//devicePtr = attachAnalogPinArray();
		break;
	}
		/*
		 case SENSOR_TYPE_DIGITAL_PIN_ARRAY: {
		 //devicePtr = attachDigitalPinArray();
		 break;
		 }
		 case SENSOR_TYPE_PULSE: {
		 //devicePtr = attachPulse();
		 break;
		 }
		 */
	case DEVICE_TYPE_ULTRASONIC: {
		//devicePtr = attachUltrasonic();
		break;
	}
	case DEVICE_TYPE_STEPPER: {
		//devicePtr = attachStepper();
		break;
	}
	case DEVICE_TYPE_MOTOR: {
		//devicePtr = attachMotor();
		break;
	}
	case DEVICE_TYPE_SERVO: {
		devicePtr = new MrlServo(); //no need to pass the type here
		break;
	}
	case DEVICE_TYPE_I2C: {
		devicePtr = new MrlI2CBus();
		break;
	}
	case DEVICE_TYPE_NEOPIXEL: {
		devicePtr = new MrlNeopixel();
    break;
	}
	default: {
		// TODO: publish error message
		MrlMsg::publishDebug(F("Unknown Message Type."));
		break;
	}
	}

	// if we have a device - then attach it and call its attach method with config passed in
	// and send back a publishedAttachedDevice with its name - so Arduino-Java land knows
	// it was successfully attached
	if (devicePtr) {
		if (devicePtr->deviceAttach(config, configSize)) {
			addDevice(devicePtr);
			publishAttachedDevice(devicePtr->id, nameSize, ioCmd + 3);
		} else {
			MrlMsg::publishError(ERROR_UNKOWN_SENSOR, F("DEVICE not attached"));
			delete devicePtr;
		}
	}
}
/**
 * deviceDetach - get the device
 * if it exists delete it and remove it from the deviceList
 */
void MrlComm::deviceDetach(int id) {
	ListNode<Device*>* node = deviceList.getRoot();
	int index = 0;
	while (node != NULL) {
		if (node->data->id == id) {
			delete node->data;
			deviceList.remove(index);
      break;
		}
		node = node->next;
		index++;
	}
}
/**
 * getDevice - this method will look up a device by it's id in the device list.
 * it returns null if the device isn't found.
 */
Device* MrlComm::getDevice(int id) {
	ListNode<Device*>* node = deviceList.getRoot();
	while (node != NULL) {
		if (node->data->id == id) {
			return node->data;
		}
		node = node->next;
	}
	MrlMsg::publishError(ERROR_DOES_NOT_EXIST);
	return NULL; //returning a NULL ptr can cause runtime error
	// you'll still get a runtime error if any field, member or method not
	// defined is accessed
}
/**
 * This adds a device to the current set of active devices in the deviceList.
 * 
 * FIXME - G: I think dynamic array would work better
 * at least for the deviceList
 * TODO: KW: i think it's pretty dynamic now.
 * G: the nextDeviceId & Id leaves something to be desired - and the "index" does
 * not spin through the deviceList to find it .. a dynamic array of pointers would only
 * expand if it could not accomidate the current number of devices, when a device was
 * removed - the slot could be re-used by the next device request
 */
void MrlComm::addDevice(Device* device) {
	deviceList.add(device);
}

/***********************************************************************
 * UPDATE DEVICES BEGIN
 * updateDevices updates each type of device put on the device list
 * depending on their type.
 * This method processes each loop. Typically this "back-end"
 * processing will read data from pins, or change states of non-blocking
 * pulses, or possibly regulate a motor based on pid values read from
 * pins
 */
void MrlComm::updateDevices() {

	// update self - the first device which
	// is type Arduino
	update();

	ListNode<Device*>* node = deviceList.getRoot();
	// iterate through our device list and call update on them.
	while (node != NULL) {
		node->data->update();
		node = node->next;
	}
}

/***********************************************************************
 * UPDATE BEGIN
 * updates self - reads from the pinList both analog and digital
 * sends pin data back
 */
void MrlComm::update() {
	unsigned long now = millis();
	if ((now - lastHeartbeatUpdate > 1000) && heartbeatEnabled) {
		if (!heartbeat) {
			softReset();
			return;
		}
		heartbeat = false;
		lastHeartbeatUpdate = now;
	}
	if (pinList.size() > 0) {
		// device id for our Arduino is always 0
		MrlMsg msg(PUBLISH_SENSOR_DATA, 0); // the callback id

		// size of payload - 1 byte for address + 2 bytes per pin read
		// this is an optimization in that we send back "all" the read pin data in a
		// standard 2 byte package - digital reads don't need both bytes, but the
		// sending it all back in 1 msg and the simplicity is well worth it
		//msg.addData(pinList.size() * 3 /* 1 address + 2 read bytes */);
		msg.countData();
		msg.autoSend(57);
    ListNode<Pin*>* node = pinList.getRoot();
    // iterate through our device list and call update on them.
    unsigned int msgSent = 0;
    while (node != NULL) {
			Pin* pin = node->data;
			if (pin->rate == 0 || (now > pin->lastUpdate + (1000 / pin->rate))) {
			  pin->lastUpdate = now;
        // TODO: moe the analog read outside of thie method and pass it in!
        if (pin->type == ANALOG) {
          pin->value = analogRead(pin->address);
        } else {
          pin->value = digitalRead(pin->address);
        }
        
        // loading both analog & digital data
        msg.addData(pin->address); // 1 byte
        msg.addData16(pin->value); // 2 bytes
        msgSent++;
      }
      node = node->next;
    }
    if (msgSent) msg.sendMsg();
	}
}

unsigned int MrlComm::getCustomMsg() {
  if (customMsgSize == 0) {
    return 0;
  }
  int retval = customMsg[0];
  for (int i = 0; i < customMsgSize-1; i++) {
    customMsg[i] = customMsg[i+1];
  }
  customMsg[customMsgSize] = 0;
  customMsgSize--;
  return retval;
}

int MrlComm::getCustomMsgSize() {
  return customMsgSize;
}

