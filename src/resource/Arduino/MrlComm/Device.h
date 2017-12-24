#ifndef Device_h
#define Device_h

/***********************************************************************
 * DEVICE
 * index - unique identifier for this device (used to look up the device in the device list.)
 * type  - type of device  (SENSOR_TYPE_DIGITAL_PIN_ARRAY |  SENSOR_TYPE_ANALOG_PIN_ARRAY | SENSOR_TYPE_DIGITAL_PIN | SENSOR_TYPE_PULSE | SENSOR_TYPE_ULTRASONIC)
 * pins  - this is the list of pins that are associated with this device (Pin)
*/

class Msg;

/**
* GLOBAL DEVICE TYPES END
**********************************************************************/


class Device {
  public:
	// deviceId is supplied by a parameter in an 'attach' message
	// deviceType is supplied by the device class as a form of runtime
	// class identification (rtti)
    Device(byte deviceId, byte deviceType);
    virtual ~Device(){
      // default destructor for the device class. 
      // destructor is set as virtual to call the destructor of the subclass. 
      // destructor should be done in the subclass level
    }

    int id; // the all important id of the sensor - equivalent to the "name" - used in callbacks
    int type; // what type of device is this?
    int state; // state - single at the moment to handle all the finite states of the sensors (todo maybe this moves into the subclasses?)
    virtual void update() {}; // all devices must implement this to update their state.
    virtual void onDisconnect() {}; //all devices must implement this to react when the communication with MRL is broken

    Msg* msg; // Msg is the generated interface for all communication

};

#endif
