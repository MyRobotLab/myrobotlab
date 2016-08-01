#ifndef Device_h
#define Device_h

/***********************************************************************
 * DEVICE
 * index - unique identifier for this device (used to look up the device in the device list.)
 * type  - type of device  (DEVICE_TYPE_ARDUINO | DEVICE_TYPE_ULTRASONIC | DEVICE_TYPE_SERVO | ...)
 * pins  - this is the list of pins that are associated with this device (Pin)
*/

/***********************************************************************
* GLOBAL DEVICE TYPES BEGIN
* THESE ARE MICROCONTROLLER AGNOSTIC !
* and defined in org.myrobotlab.service.interface.Device
* These values "must" align with the Device class
* TODO - find a way to auto sync this with Device from Java-land
* TODO - should be in seperate generated file
* Device types start as 1 - so if anyone forgot to
* define their device it will error - rather default
* to a device they may not want
*/

#define DEVICE_TYPE_UNKNOWN             0

#define DEVICE_TYPE_ARDUINO			        1
#define DEVICE_TYPE_ULTRASONIC          4
#define DEVICE_TYPE_STEPPER             5
#define DEVICE_TYPE_MOTOR               6
#define DEVICE_TYPE_SERVO               7
#define DEVICE_TYPE_I2C                 8
#define DEVICE_TYPE_NEOPIXEL            9


/**
* GLOBAL DEVICE TYPES END
**********************************************************************/


class Device {
  public:
    Device(int deviceType);
    virtual ~Device(){
      // default destructor for the device class. 
      // destructor is set as virtual to call the destructor of the subclass. 
      // destructor should be done in the subclass level
    }
    unsigned int id; // the all important id of the sensor - equivalent to the "name" - used in callbacks
    int type; // what type of device is this?
    int state; // state - single at the moment to handle all the finite states of the sensors (todo maybe this moves into the subclasses?)
    // GroG - I think its good here - a uniform state description across all devices is if they are DEVICE_STATE_ACTIVE or DEVICE_STATE_DEACTIVE
    // subclasses can/should define their os substate - eg ULTRASONIC_STATE_WAITING_PULSE etc..
    virtual void update() {}; // all devices must implement this to update their state.
    // the universal attach - follows Java-Land Controller.deviceAttach method
    virtual bool deviceAttach(unsigned char config[], int configSize);
    static unsigned int nextDeviceId;
  protected:
    void attachDevice();
};

#endif
