/**
* MRLComm.c
* -----------------
* This file is part of MyRobotLab.
* (myrobotlab.org)
*
* Enjoy !
* @authors
* GroG
* Kwatters
* Mats
* calamity
* and many others...
*
* MRL Protocol definition
* -----------------
* MAGIC_NUMBER|NUM_BYTES|FUNCTION|DATA0|DATA1|....|DATA(N)
*              NUM_BYTES - is the number of bytes after NUM_BYTES to the end
*
* more info - http://myrobotlab.org/content/myrobotlab-api
*
* General Concept
* -----------------
* Arduino is a slave process to MyRobotLab Arduino Service - this file receives
* commands and sends back data.
* Refactoring has made MRLComm.c far more general
* there are only 2 "types" of things - controllers and pins - or writers and readers
* each now will have sub-types
*
* Controllers
* -----------------
* digital pins, pwm, pwm/dir dc motors, pwm/pwm dc motors
*
* Sensors
* -----------------
* digital polling pins, analog polling pins, range pins, oscope, trigger events
*
* Combination
* -----------------
* pingdar, non-blocking pulsin
*
* Requirements: MyRobotLab running on a computer & a serial connection
*
*  TODO - need a method to identify type of board http://forum.arduino.cc/index.php?topic=100557.0
*  TODO - getBoardInfo() - returns board info !
*  TODO - getPinInfo() - returns pin info !
*  TODO - implement with std::vector vs linked list - https://github.com/maniacbug/StandardCplusplus/blob/master/README.md
*  TODO - make MRLComm a c++ library
*/

// Included as a 3rd party arduino library from here: https://github.com/ivanseidel/LinkedList/
// #include <LinkedList.h>
/*
  LinkedList.h - V1.1 - Generic LinkedList implementation
  Works better with FIFO, because LIFO will need to
  search the entire List to find the last one;
  For instructions, go to https://github.com/ivanseidel/LinkedList
  Created by Ivan Seidel Gomes, March, 2013.
  Released into the public domain.
*/

#ifndef LinkedList_h
#define LinkedList_h

template<class T>
struct ListNode
{
  T data;
  ListNode<T> *next;
};

template <typename T>
class LinkedList{

protected:
  int _size;
  ListNode<T> *root;
  ListNode<T> *last;
  // Helps "get" method, by saving last position
  ListNode<T> *lastNodeGot;
  int lastIndexGot;
  // isCached should be set to FALSE
  // everytime the list suffer changes
  bool isCached;
  ListNode<T>* getNode(int index);

public:
  LinkedList();
  ~LinkedList();
  /* Returns current size of LinkedList */
  virtual int size();
  /* Adds a T object in the specified index;
    Unlink and link the LinkedList correcly;
    Increment _size */
  virtual bool add(int index, T);
  /* Adds a T object in the end of the LinkedList;
    Increment _size; */
  virtual bool add(T);
  /* Adds a T object in the start of the LinkedList;
    Increment _size; */
  virtual bool unshift(T);
  /* Set the object at index, with T;
    Increment _size; */
  virtual bool set(int index, T);
  /* Remove object at index;
    If index is not reachable, returns false;
    else, decrement _size */
  virtual T remove(int index);
  /* Remove last object; */
  virtual T pop();
  /* Remove first object; */
  virtual T shift();
  /* Get the index'th element on the list;
    Return Element if accessible,
    else, return false; */
  virtual T get(int index);
  /* Clear the entire array */
  virtual void clear();
};

// Initialize LinkedList with false values
template<typename T>
LinkedList<T>::LinkedList() {
  root=false;
  last=false;
  _size=0;
  lastNodeGot = root;
  lastIndexGot = 0;
  isCached = false;
}

// Clear Nodes and free Memory
template<typename T>
LinkedList<T>::~LinkedList() {
  ListNode<T>* tmp;
  while(root!=false) {
    tmp=root;
    root=root->next;
    delete tmp;
  }
  last = false;
  _size=0;
  isCached = false;
}

/* Actualy "logic" coding */
template<typename T>
ListNode<T>* LinkedList<T>::getNode(int index) {
  int _pos = 0;
  ListNode<T>* current = root;
  // Check if the node trying to get is
  // immediatly AFTER the previous got one
  if(isCached && lastIndexGot <= index) {
    _pos = lastIndexGot;
    current = lastNodeGot;
  }
  while(_pos < index && current) {
    current = current->next;
    _pos++;
  }
  // Check if the object index got is the same as the required
  if(_pos == index) {
    isCached = true;
    lastIndexGot = index;
    lastNodeGot = current;
    return current;
  }
  return false;
}

template<typename T>
int LinkedList<T>::size() {
  return _size;
}

template<typename T>
bool LinkedList<T>::add(int index, T _t) {
  if(index >= _size)
    return add(_t);
  if(index == 0)
    return unshift(_t);
  ListNode<T> *tmp = new ListNode<T>(),
    *_prev = getNode(index-1);
  tmp->data = _t;
  tmp->next = _prev->next;
  _prev->next = tmp;
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::add(T _t) {
  ListNode<T> *tmp = new ListNode<T>();
  tmp->data = _t;
  tmp->next = false;
  if(root) {
    // Already have elements inserted
    last->next = tmp;
    last = tmp;
  } else {
    // First element being inserted
    root = tmp;
    last = tmp;
  }
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::unshift(T _t) {
  if(_size == 0)
    return add(_t);
  ListNode<T> *tmp = new ListNode<T>();
  tmp->next = root;
  tmp->data = _t;
  root = tmp;
  _size++;
  isCached = false;
  return true;
}

template<typename T>
bool LinkedList<T>::set(int index, T _t) {
  // Check if index position is in bounds
  if(index < 0 || index >= _size)
    return false;
  getNode(index)->data = _t;
  return true;
}

template<typename T>
T LinkedList<T>::pop() {
  if(_size <= 0)
    return T();
  isCached = false;
  if(_size >= 2) {
    ListNode<T> *tmp = getNode(_size - 2);
    T ret = tmp->next->data;
    delete(tmp->next);
    tmp->next = false;
    last = tmp;
    _size--;
    return ret;
  } else {
    // Only one element left on the list
    T ret = root->data;
    delete(root);
    root = false;
    last = false;
    _size = 0;
    return ret;
  }
}

template<typename T>
T LinkedList<T>::shift() {
  if (_size <= 0)
    return T();
  if (_size > 1) {
    ListNode<T> *_next = root->next;
    T ret = root->data;
    delete(root);
    root = _next;
    _size --;
    isCached = false;
    return ret;
  } else {
    // Only one left, then pop()
    return pop();
  }
}

template<typename T>
T LinkedList<T>::remove(int index) {
  if (index < 0 || index >= _size) {
    return T();
  }
  if(index == 0)
    return shift();
  if (index == _size-1) {
    return pop();
  }
  ListNode<T> *tmp = getNode(index - 1);
  ListNode<T> *toDelete = tmp->next;
  T ret = toDelete->data;
  tmp->next = tmp->next->next;
  delete(toDelete);
  _size--;
  isCached = false;
  return ret;
}

template<typename T>
T LinkedList<T>::get(int index){
  ListNode<T> *tmp = getNode(index);
  return (tmp ? tmp->data : T());
}

template<typename T>
void LinkedList<T>::clear() {
  while(size() > 0)
    shift();
}

#endif

#include <Servo.h>
#define WIRE Wire
#include <Wire.h>

// TODO: this isn't ready for an official bump to mrl comm 35
// when it's ready we can update ArduinoMsgCodec  (also need to see why it's not publishing "goodtimes" anymore.)
#define MRLCOMM_VERSION         35

// serial protocol functions
#define MAGIC_NUMBER            170 // 10101010

/**
* FIXME - first rule of generate club is: whole file should be generated
* so this needs to be turned itno a .h if necessary - but the manual munge
* should be replaced
*/

// ----- MRLCOMM FUNCTION GENERATED INTERFACE BEGIN -----------
///// INO GENERATED DEFINITION BEGIN //////
// {publishMRLCommError Integer}
#define PUBLISH_MRLCOMM_ERROR		1
// {getVersion}
#define GET_VERSION		2
// {publishVersion Integer}
#define PUBLISH_VERSION		3
// {addSensorDataListener SensorDataListener}
#define ADD_SENSOR_DATA_LISTENER		4
// {analogReadPollingStart Integer Integer}
#define ANALOG_READ_POLLING_START		5
// {analogReadPollingStop int}
#define ANALOG_READ_POLLING_STOP		6
// {analogWrite int int}
#define ANALOG_WRITE		7
// {attachDevice Device}
#define ATTACH_DEVICE		8
// {createDevice int int String}
#define CREATE_DEVICE		9
// {digitalReadPollingStart Integer Integer}
#define DIGITAL_READ_POLLING_START		10
// {digitalReadPollingStop int}
#define DIGITAL_READ_POLLING_STOP		11
// {digitalWrite int int}
#define DIGITAL_WRITE		12
// {fixPinOffset Integer}
#define FIX_PIN_OFFSET		13
// {i2cRead int int byte[] int}
#define I2C_READ		14
// {i2cWrite int int byte[] int}
#define I2C_WRITE		15
// {i2cWriteRead int int byte[] int byte[] int}
#define I2C_WRITE_READ		16
// {intsToString int[] int int}
#define INTS_TO_STRING		17
// {motorAttach String int}
#define MOTOR_ATTACH		18
// {motorDetach MotorControl}
#define MOTOR_DETACH		19
// {motorMove MotorControl}
#define MOTOR_MOVE		20
// {motorMoveTo MotorControl}
#define MOTOR_MOVE_TO		21
// {motorReset MotorControl}
#define MOTOR_RESET		22
// {motorStop MotorControl}
#define MOTOR_STOP		23
// {pinMode int String}
#define PIN_MODE		24
// {publishAttachedDevice Device}
#define PUBLISH_ATTACHED_DEVICE		25
// {publishDebug String}
#define PUBLISH_DEBUG		26
// {publishMessageAck}
#define PUBLISH_MESSAGE_ACK		27
// {publishPin Pin}
#define PUBLISH_PIN		28
// {publishPulse Long}
#define PUBLISH_PULSE		29
// {publishPulseStop Integer}
#define PUBLISH_PULSE_STOP		30
// {publishSensorData SensorData}
#define PUBLISH_SENSOR_DATA		31
// {publishServoEvent Integer}
#define PUBLISH_SERVO_EVENT		32
// {publishStatus Long Integer}
#define PUBLISH_STATUS		33
// {publishTrigger Pin}
#define PUBLISH_TRIGGER		34
// {pulse int int int int}
#define PULSE		35
// {pulseStop}
#define PULSE_STOP		36
// {releaseDevice int int}
#define RELEASE_DEVICE		37
// {sensorPollingStart Integer}
#define SENSOR_POLLING_START		38
// {sensorPollingStop Integer}
#define SENSOR_POLLING_STOP		39
// {servoAttach Servo Integer}
#define SERVO_ATTACH		40
// {servoDetach Servo}
#define SERVO_DETACH		41
// {servoEventsEnabled Servo}
#define SERVO_EVENTS_ENABLED		42
// {servoSweepStart Servo}
#define SERVO_SWEEP_START		43
// {servoSweepStop Servo}
#define SERVO_SWEEP_STOP		44
// {servoWrite Servo}
#define SERVO_WRITE		45
// {servoWriteMicroseconds Servo}
#define SERVO_WRITE_MICROSECONDS		46
// {setDebounce int}
#define SET_DEBOUNCE		47
// {setDebug boolean}
#define SET_DEBUG		48
// {setDigitalTriggerOnly Boolean}
#define SET_DIGITAL_TRIGGER_ONLY		49
// {setLoadTimingEnabled boolean}
#define SET_LOAD_TIMING_ENABLED		50
// {setPWMFrequency Integer Integer}
#define SET_PWMFREQUENCY		51
// {setSampleRate int}
#define SET_SAMPLE_RATE		52
// {setSerialRate int}
#define SET_SERIAL_RATE		53
// {setServoSpeed Servo}
#define SET_SERVO_SPEED		54
// {setTrigger int int int}
#define SET_TRIGGER		55
// {softReset}
#define SOFT_RESET		56
///// INO GENERATED DEFINITION END //////

// ----- MRLCOMM FUNCTION GENERATED INTERFACE END -----------

///// INO GENERATED DEFINITION END //////

// TODO: the max message size isn't auto generated but
// it is in ArduinioMsgCodec
#define MAX_MSG_SIZE 64

// servo event types
// ===== published sub-types based on device type begin ===
#define  SERVO_EVENT_STOPPED          1
#define  SERVO_EVENT_POSITION_UPDATE  2
// ===== published sub-types based on device type begin ===

// ------ error types ------
#define ERROR_SERIAL            1
#define ERROR_UNKOWN_CMD        2
#define ERROR_ALREADY_EXISTS    3
#define ERROR_DOES_NOT_EXIST    4
#define ERROR_UNKOWN_SENSOR     5

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

#define SENSOR_TYPE_ANALOG_PIN_ARRAY  	1
#define SENSOR_TYPE_DIGITAL_PIN_ARRAY  	2
#define SENSOR_TYPE_PULSE  				      3
#define SENSOR_TYPE_ULTRASONIC  		    4

#define DEVICE_TYPE_STEPPER  			      5
#define DEVICE_TYPE_MOTOR  				      6
#define DEVICE_TYPE_SERVO  				      7
#define DEVICE_TYPE_I2C                 8
/**
* GLOBAL DEVICE TYPES END
**********************************************************************/

// ECHO FINITE STATE MACHINE - NON BLOCKING PULSIN
#define ECHO_STATE_START                    1
#define ECHO_STATE_TRIG_PULSE_BEGIN         2
#define ECHO_STATE_TRIG_PULSE_END           3
#define ECHO_STATE_MIN_PAUSE_PRE_LISTENING  4
#define ECHO_STATE_LISTENING                5
#define ECHO_STATE_GOOD_RANGE               6
#define ECHO_STATE_TIMEOUT                  7

/***********************************************************************
 * BOARD TYPE
 */
#define BOARD_TYPE_UNKNOWN 0
#define BOARD_TYPE_MEGA    1
#define BOARD_TYPE_UNO     2

#if defined(ARDUINO_AVR_MEGA2560)
  #define BOARD BOARD_TYPE_MEGA
#elif defined(ARDUINO_AVR_UNO)
  #define BOARD BOARD_TYPE_UNO
#else
  #define BOARD BOARD_TYPE_UNKNOWN
#endif

/***********************************************************************
 * PIN - This class represents one of the pins on the arduino and it's
 * status in MRLComm.
 */
class Pin {
  public:
    // default constructor for a pin?
    // at a minimum we need type and address A0 D4 etc...
    Pin(int t, int addr) {
      type = t;
      address = addr;
    };
    int type; // might be useful in control
    int address; // pin #
    int value;
    int state; // state of the pin - not sure if needed - reading | writing | some other state ?
    // int readModulus; // rate of reading or publish sensor data
    int debounce; // long lastDebounceTime - minDebounceTime
    // number of reads ?
    unsigned long target;
    // TODO: review/remove move the following members
    // to support pulse.
    int count;
    // remove me
    int rate;
    // remove me,
    int rateModulus;
};

/***********************************************************************
 * DEVICE
 * index - unique identifier for this device (used to look up the device in the device list.)
 * type  - type of device  (SENSOR_TYPE_DIGITAL_PIN_ARRAY |  SENSOR_TYPE_ANALOG_PIN_ARRAY | SENSOR_TYPE_DIGITAL_PIN | SENSOR_TYPE_PULSE | SENSOR_TYPE_ULTRASONIC)
 * pins  - this is the list of pins that are associated with this device (Pin)
*/

class Device {
  public:
    Device(int deviceType, int deviceId) {
      type = deviceType;
      index=deviceId;
    }
    ~Device(){
      while(pins.size()>0){
        delete pins.pop();
      }
    }
    int index; // the all important index of the sensor - equivalent to the "name" - used in callbacks
    int type; //
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0 to many
    // TODO: review all members below here for potential removal as a result of refactoring.
    // bool isActive; - not currently needed as inactive sensors are removed from the deviceList
    // int readModulus; // rate of reading or publish sensor data
    int state; // state - single at the moment to handle all the finite states of the sensor
   //  LinkedList<int> config; // additional memory for the sensor if needed

    int step;
    int min;
    int max;

    int echoPin;
    int trigPin;
    unsigned long ts;
    unsigned long lastValue;
    int timeoutUS;
    int count;
    int id;
};

/**
 * Servo Device
 */
class MrlServo : public Device {
  public:
    Servo* servo; // servo pointer - in case our device is a servo
    int pin;
    bool isMoving;
    bool isSweeping;
    int targetPos;
    int currentPos;
    int speed; // servos have a "speed" associated with them that's not part of the base servo driver
    bool eventsEnabled;

    MrlServo(int p, int deviceId) : Device(DEVICE_TYPE_SERVO, deviceId) {
       pin = p;
       isMoving = false;
       isSweeping = false;
       // TODO: target/curent position?
       // create the servo
       servo = new Servo();
       servo->attach(pin);
       eventsEnabled = false;
    }
    ~MrlServo() {
      servo->detach();
      delete servo;
    }

};

/**
 * Motor Device
 */
class MrlMotor : public Device {
  // details of the different motor controls/types
  public:
    MrlMotor(int deviceId) : Device(DEVICE_TYPE_MOTOR, deviceId) {

    }
};

/**
 * Stepper Device
 */
class MrlStepper : public Device {
  // details of the different motor controls/types
  public:
    MrlStepper(int deviceId) : Device(DEVICE_TYPE_STEPPER, deviceId) {

    }
};

/**
 * Analog Pin Array Sensor
 */
class MrlAnalogPinArray : public Device {
  // int pins[];
  public:
    MrlAnalogPinArray(int deviceId) : Device(SENSOR_TYPE_ANALOG_PIN_ARRAY, deviceId) {
    //pins = sensorPins;
    //for (int i = 0; i < pins.size(); i++) {
      // TODO: is this Analog or Digital?
    //  pinMode(i, INPUT);
    //}
    }
  // specific stuffs for analog pins (sample rates?)
};

/**
 * Digital Pin Array Sensor
 */
class MrlDigitalPinArray : public Device {
  public:
    // config such as if they should publish the value in every loop
    // or only on a state change.
    MrlDigitalPinArray(int deviceId) : Device(SENSOR_TYPE_DIGITAL_PIN_ARRAY, deviceId) {
    //pins = sensorPins;
    //for (int i = 0; i < pins.size(); i++) {
      // TODO: is this Analog or Digital?
    //  pinMode(i, INPUT);
    //}
    }
};

/**
 * Pulse Sensor Device
 */
class MrlPulse : public Device {
  public:
    MrlPulse(int deviceId) : Device(SENSOR_TYPE_PULSE, deviceId) {
    }
};

/**
 * Ultrasonic Sensor
 */
class MrlUltrasonic : public Device {
  public:
    MrlUltrasonic(int deviceId) : Device(SENSOR_TYPE_ULTRASONIC, deviceId) {
    }
};

/**
 * I2C device
 */
class MrlI2CDevice : public Device {
  public:
    MrlI2CDevice(int deviceId) : Device(DEVICE_TYPE_I2C, deviceId) {
    }
};


/***********************************************************************
 * GLOBAL VARIABLES
 * TODO - work on reducing globals and pass as parameters
*/

// The mighty device List.  This contains all active devices that are attached to the arduino.
LinkedList<Device*> deviceList;
int nextDeviceId = 0;

// MRLComm message buffer and current count from serial port ( MAGIC | MSGSIZE | FUNCTION | PAYLOAD ...
int msgSize = 0; // the NUM_BYTES of current message (second byte of mrlcomm protocol)
unsigned char ioCmd[MAX_MSG_SIZE];  // message buffer for all inbound messages
int byteCount = 0;
unsigned long loopCount = 0; // main loop count
unsigned int debounceDelay = 50; // in ms
// performance metrics  and load timing
bool loadTimingEnabled = false;
int loadTimingModulus = 1000; // the frequency in which to report the load timing metrics (in number of main loops)
unsigned long lastMicros = 0; // timestamp of last loop (if stats enabled.)

// sensor sample rate
unsigned int sampleRate = 1; // 1 - 65,535 modulus of the loopcount - allowing you to sample less

// define any functions that pass structs into them.
void sendServoEvent(Device* s, int eventType);

// Switch to know it the i2c bus has been initiated or not
/** TODO Move to the device, in case several buses can be allocated
    some versions of wire allows for that */
bool i2cInitiated = false;

// global debug setting, if set to true publishDebug will write to the serial port.
bool debug = false;

/***********************************************************************
 * DEVICE LIST ACCESS METHODS BEGIN
 * basic crud operations for devices to seperate the implementation
 * details of the data structure containing all the devices
 */
Device* getDeviceByIndex(int id);
void removeDevice(int id);
int addDevice(Device*);
/**
 * DEVICE LIST ACCESS METHODS END
 **********************************************************************/

/***********************************************************************
 * STANDARD ARDUINO BEGIN
*/
void setup() {
  Serial.begin(115200);        // connect to the serial port
  while (!Serial){};
  // TODO: do this before we start the serial port?
  softReset();
  // publish version on startup so it's immediately available for mrl.
  // TODO: see if we can purge the current serial port buffers
  publishVersion();
  // TODO: see if we can publish the board type (uno/mega?)
  // publishBoardType();
}

// This is the main loop that the arduino runs.
void loop() {
  // increment how many times we've run
  ++loopCount;
  // get a command and process it from the serial port (if available.)
  if (getCommand()) {
    processCommand();
  }
  // update devices
  updateDevices();
  // update memory & timing
  updateStatus();
  // update the timestamp for this loop.
  lastMicros = micros();
} // end of big loop

/**
 * STANDARD ARDUINO END
**********************************************************************/

/***********************************************************************
 * UTILITY METHODS BEGIN
 */
unsigned long toUnsignedLongfromBigEndian(unsigned char* buffer, int start) {
  return (((unsigned long)buffer[start] << 24) +
          ((unsigned long)buffer[start + 1] << 16) +
          (buffer[start + 2] << 8) + buffer[start + 3]);
}

int getFreeRam()
{
  extern int __heap_start, *__brkval;
  int v;
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
}

void softReset() {
  while(deviceList.size()>0){
    delete deviceList.pop();
  }
  //resetting global var to default
  loopCount = 0;
  loadTimingModulus=1000;
  loadTimingEnabled = false;
  sampleRate = 1;
  debounceDelay = 50;
  nextDeviceId = 0;
}

/**
 * FIXME - G: I think dynamic array would work better
 * at least for the deviceList
 */
int addDevice(Device* device) {
  deviceList.add(device);
  return device->index;
}

/**
 * getDeviceByIndex - this method will look up a device by it's index in the device list.
 * it returns null if the device isn't found.
 */
Device* getDeviceByIndex(int index) {
  // TODO: more effecient lookup..
  int numDevices = deviceList.size();
  for (int i = 0; i < numDevices; i++) {
    Device* dev=deviceList.get(i);
    if (dev->index == index) {
      // it was found.
      return dev;
    }
  }
  return NULL;
}

/**
 * UTILITY METHODS END
 ***********************************************************************/

/***********************************************************************
 * SERIAL METHODS BEGIN
 * TODO - add text api
 */
bool getCommand() {
  // handle serial data begin
  int bytesAvailable = Serial.available();
  if (bytesAvailable > 0) {
    publishDebug("RXBUFF:" + String(bytesAvailable));
    // now we should loop over the available bytes .. not just read one by one.
    for (int i = 0 ; i < bytesAvailable; i++) {
      // read the incoming byte:
      unsigned char newByte = Serial.read();
      publishDebug("RX:" + String(newByte));
      ++byteCount;
      // checking first byte - beginning of message?
      if (byteCount == 1 && newByte != MAGIC_NUMBER) {
        sendError(ERROR_SERIAL);
        // reset - try again
        byteCount = 0;
        // return false;
      }
      if (byteCount == 2) {
        // get the size of message
        // todo check msg < 64 (MAX_MSG_SIZE)
        if (newByte > 64){
          // TODO - send error back
          byteCount = 0;
          continue; // GroG - I guess  we continue now vs return false on error conditions?
        }
        msgSize = newByte;
      }
      if (byteCount > 2) {
        // fill in msg data - (2) headbytes -1 (offset)
        ioCmd[byteCount - 3] = newByte;
      }
      // if received header + msg
      if (byteCount == 2 + msgSize) {
        // we've reach the end of the command, just return true .. we've got it
        return true;
      }
    }
  } // if Serial.available
  // we only partially read a command.  (or nothing at all.)
  return false;
}

// This function will switch the current command and call
// the associated function with the command
void processCommand() {
  switch (ioCmd[0]) {
  // === system pass through begin ===
  case DIGITAL_WRITE:
    digitalWrite(ioCmd[1], ioCmd[2]);
    break;
  case ANALOG_WRITE:
    analogWrite(ioCmd[1], ioCmd[2]);
    break;
  case PIN_MODE:
    pinMode(ioCmd[1], ioCmd[2]);
    break;
  case SERVO_ATTACH:
    // this a user command to Servo.attach
    // not to be confused with attachServo which is
    // MRLComm framework request to attach a device
    servoAttach();
    break;
  case SERVO_SWEEP_START:
    servoStartSweep();
    break;
  case SERVO_SWEEP_STOP:
    servoStopSweep();
    break;
  case SERVO_EVENTS_ENABLED:
    servoEventsEnabled();
    break;
  case SERVO_WRITE:
    servoWrite();
    break;
  case PUBLISH_SERVO_EVENT:
    publishServoEvent();
    break;
  case SERVO_WRITE_MICROSECONDS:
    servoWriteMicroseconds();
    break;
  case SET_SERVO_SPEED:
    setServoSpeed();
    break;
  case SERVO_DETACH:
    servoDetach();
    break;
  case SET_LOAD_TIMING_ENABLED:
    setLoadTimingEnabled();
    break;
  case SET_PWMFREQUENCY:
    setPWMFrequency(ioCmd[1], ioCmd[2]);
    break;
  case ANALOG_READ_POLLING_START:
    analogReadPollingStart();
    break;
  case ANALOG_READ_POLLING_STOP:
    analogReadPollingStop();
    break;
  case DIGITAL_READ_POLLING_START:
    digitalReadPollingStart();
    break;
  case DIGITAL_READ_POLLING_STOP:
    digitalReadPollingStop();
  case PULSE:
    pulse();
    break;
  case PULSE_STOP:
    pulseStop();
    break;
  case SET_TRIGGER:
    setTrigger();
    break;
  case SET_DEBOUNCE:
    setDebounce();
    break;
  case SET_DIGITAL_TRIGGER_ONLY:
    setDigitalTriggerOnly();
    break;
  case SET_SERIAL_RATE:
    setSerialRate();
    break;
  case GET_VERSION:
    getVersion();
    break;
  case SET_SAMPLE_RATE:
    setSampleRate();
    break;
  case SOFT_RESET:
    softReset();
    break;
  case ADD_SENSOR_DATA_LISTENER:
    addSensorDataListener();
    break;
  case SENSOR_POLLING_START:
    sensorPollingStart();
    break;
  case SENSOR_POLLING_STOP:
    sensorPollingStop();
    break;
  // Start of i2c read and writes
  case I2C_READ:
    i2cRead();
    break;
  case I2C_WRITE:
    i2cWrite();
    break;
  case I2C_WRITE_READ:
    i2cWriteRead();
    break;
  case SET_DEBUG:
    debug = ioCmd[1];
    if (debug)
    {
      publishDebug(F("Debug logging enabled."));
    }
    break;
  default:
    sendError(ERROR_UNKOWN_CMD);
    break;
  } // end switch
  // ack that we got a command (should we ack it first? or after we process the command?)
  sendCommandAck();
  // reset command buffer to be ready to receive the next command.
  memset(ioCmd, 0, sizeof(ioCmd));
  byteCount = 0;
} // process Command

/**
 * SERIAL METHODS END
 **********************************************************************/

/***********************************************************************
 * CONTROL METHODS BEGIN
 * TODO - add text api
 */

void sensorPollingStart() {
  // TODO: implement me.
}

void sensorPollingStop() {
  // TODO: implement me.
}

// Start of I2CControl interface methods
//
// Return data in this format
// PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATA_SIZE | DATA ....
/** TODO Add device index to the returned data
//  I hope only one device is needed for the i2c bus in MRLComm,
//  not one for each i2c device on the bus.
//  That information is better handled in Arduino.java
*/
void i2cRead(){
  wireBegin();
  WIRE.beginTransmission(ioCmd[1]); // address to the i2c device
  WIRE.write(ioCmd[2]);             // device memory address to read from
  WIRE.endTransmission();
  int answer = WIRE.requestFrom((uint8_t)ioCmd[1], (uint8_t)ioCmd[3]); // reqest a number of bytes to read
  if(answer==0){
    //Report an error with I2C communication by returning a 0 length data size
    //i.e a message size if 1 byte containing only PUBLISH_SENSOR_DATA
     // Start an mrlcomm message
    Serial.write(MAGIC_NUMBER);
    // size of the mrlcomm message
  	Serial.write(1);
  	// mrlcomm function
  	Serial.write(PUBLISH_SENSOR_DATA);
  }
  else{
     // Start an mrlcomm message
    Serial.write(MAGIC_NUMBER);
    // size of the mrlcomm message
  	Serial.write(1 + ioCmd[3]);
  	// mrlcomm function
  	Serial.write(PUBLISH_SENSOR_DATA);
  	Serial.write(0);//get the DEVICE_INDEX
  	// I2C device are only sending back data, some more identifier should be added
  	//return the request bytes, incomplete message will be padded with 0xFF bytes
  	Serial.write(answer);
    for (int i = 1; i<answer; i++){
      Serial.write(Wire.read());
    }
  }
}

void i2cWrite(){
  wireBegin();
  WIRE.beginTransmission(ioCmd[1]);   // address to the i2c device
  WIRE.write(ioCmd[2]);               // device memory address to write to
  for (int i = 3; i < msgSize; i++) { // data to write
    WIRE.write(ioCmd[i]);
  }
  WIRE.endTransmission();
}

void i2cWriteRead(){
  /** TODO implement me */
}
// Method to make sure that WIRE.begin() is executed exactly one time
// before any other i2c communication
void wireBegin() {
  if (!i2cInitiated){
    WIRE.begin();
    i2cInitiated = true;
  }
}
// End of I2CControl interface methods

// MRL Command helper methods below:
// GET_VERSION
void getVersion() {
  // call publish version to talk to the serial port.
  publishVersion();
}

// SERVO_WRITE_MICROSECONDS
void servoWriteMicroseconds() {
  // TODO - incorporate into speed control etc
  // normalize - currently by itself doesn't effect events
  // nor is it involved in speed control
  // TODO: this is wrong need to lookup device by id which means iterating the device list.
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  if (s->servo != 0) {
    // 1500 midpoint
    s->servo->writeMicroseconds(ioCmd[2]);
  }
}

// SET_SERVO_SPEED
void setServoSpeed() {
  // setting the speed of a servo
  // TODO: this is wrong need to lookup device by id which means iterating the device list.
  MrlServo* servo = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  servo->speed = ioCmd[2];
}

// SERVO_DETACH
void servoDetach() {
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  if (s->servo != 0) {
    s->servo->detach();
  }
  detachDevice(ioCmd[1]);
}

// SET_LOAD_TIMING_ENABLED
void setLoadTimingEnabled() {
  loadTimingEnabled = ioCmd[1];
  //loadTimingModulus = ioCmd[2];
  loadTimingModulus = 1;
}

// SET_PWMFREQUENCY
void setPWMFrequency(int address, int prescalar) {
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

// TODO :remove this method. we don't need it anymore.
//void analogReadPollingStartNew() {
  // TODO: do we care about this pinIndex at all?
//  int sensorIndex = ioCmd[1]; // + DIGITAL_PIN_COUNT / DIGITAL_PIN_OFFSET
  // create a new sensor with 1 pin.
//  sensor s = sensor();
//  s.index = sensorIndex;
  // create the pin for this sensor
//  Pin p = Pin();
//  p.isActive = true;
//  p.address = ioCmd[2];
  // add the pin to the sensor
//  s.pins.add(p);
  // add the sensor to the global list of sensors.
//  deviceList.add(s);
//}

// ANALOG_READ_POLLING_START
void analogReadPollingStart() {
  // TODO: remove this method..
  // if you have a device attached,and it's a pin, implicitly, we're polling.
  //int pinIndex = ioCmd[1]; // + DIGITAL_PIN_COUNT / DIGITAL_PIN_OFFSET
  //Pin& pin = pins[pinIndex];
  // TODO: remove this method and only use sensorAttach ..
  //pin.sensorIndex = 0; // FORCE ARDUINO TO BE OUR SERVICE - DUNNO IF THIS IS GOOD/BAD
  //pin.sensorType = SENSOR_TYPE_ANALOG_PIN_READER; // WIERD - mushing of roles/responsibilities
  //pin.isActive = true;
  //pin.rateModulus= (ioCmd[2] << 8)+ioCmd[3];
}

// ANALOG_READ_POLLING_STOP
void analogReadPollingStop() {
  // TODO: remove this method and use detachDevice() / removeDevice?()
  //Pin& pin = pins[ioCmd[1]];
  //pin.isActive = false;
}

// DIGITAL_READ_POLLING_START
void digitalReadPollingStart() {
  // TODO: remove this and replace iwth attachDevice()
  //int pinIndex = ioCmd[1]; // + DIGITAL_PIN_COUNT / DIGITAL_PIN_OFFSET
  //Pin& pin = pins[pinIndex];
  //pin.sensorIndex = 0; // FORCE ARDUINO TO BE OUR SERVICE - DUNNO IF THIS IS GOOD/BAD
  //pin.sensorType = SENSOR_TYPE_DIGITAL_PIN_READER; // WIERD - mushing of roles/responsibilities
  //pin.isActive = true;
  //pin.rateModulus=(ioCmd[2] << 8) + ioCmd[3];
}

// PULSE
void pulse() {
  // get pin from index
  Device* d = (MrlPulse*)deviceList.get(ioCmd[1]);
  Pin* pin = d->pins.get(0);
  // FIXME - this has to unload a Long !!!
  pin->count = 0;
  pin->target = toUnsignedLongfromBigEndian(ioCmd, 2);
  pin->rate = ioCmd[6];
  pin->rateModulus = ioCmd[7];
  // pin.isActive = true;
  pin->state = PUBLISH_SENSOR_DATA;
  //addNewValue(activePins, activePinCount, ioCmd[1]);
  //int pin = ioCmd[1];
  //addNewValue(digitalReadPin, digitalReadPollingPinCount, pin);
  // this is the same as digitalWrite except
  // we can keep track of the number of pulses
  //break;
}

// PULSE
void pulseStop() {
  Device* d = (MrlPulse*)deviceList.get(ioCmd[1]);
  Pin* pin = d->pins.get(0);
  // FIXME - this has to unload a Long !!!
  pin->state = PUBLISH_PULSE_STOP;
  //removeAndShift(activePins, activePinCount, ioCmd[1]);
}

// digital_READ_POLLING_STOP
void digitalReadPollingStop() {
  Device* d = deviceList.get(ioCmd[1]);
  Pin* pin = d->pins.get(0);
  // pin.isActive = false;
  //int pin = ioCmd[1];
  //removeAndShift(digitalReadPin, digitalReadPollingPinCount, pin);
  //break;
  // FIXME - these should just be attributes of the pin
}

// SET_TRIGGER
void setTrigger() {
  // NOT IMPLEMENTED
  // FIXME !!! - you need 1. a complete pin list !!!   analog & digital should be defined by attribute not
  // data structure !!!  if (pin.type == ??? if needed
  // TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
  //analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
  //++analogReadPollingPinCount;
}

// SET_DEBOUNCE
void setDebounce() {
  // default debounceDelay = 50;
  debounceDelay = ((ioCmd[1] << 8) + ioCmd[2]);
}

// SET_DIGITAL_TRIGGER_ONLY
void setDigitalTriggerOnly() {
  // NOT IMPLEMENTED
  //digitalTriggerOnly = ioCmd[1];
}

// SET_SERIAL_RATE
void setSerialRate() {
  Serial.end();
  delay(500);
  Serial.begin(ioCmd[1]);
}

// SET_SAMPLE_RATE
void setSampleRate() {
  // 2 byte int - valid range 1-65,535
  sampleRate = (ioCmd[1] << 8) + ioCmd[2];
  if (sampleRate == 0) {
    sampleRate = 1;
  } // avoid /0 error - FIXME - time estimate param
}

// SERVO_START_SWEEP
void servoStartSweep() {
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  s->min = ioCmd[2];
  s->max = ioCmd[3];
  s->step = ioCmd[4];
  s->isMoving = true;
  s->isSweeping = true;
}

// SERVO_STOP_SWEEP
void servoStopSweep() {
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  s->isMoving = false;
  s->isSweeping = false;
}

// SERVO_EVENTS_ENABLED
void servoEventsEnabled() {
  // Not implemented.
}

// SERVO_WRITE
void servoWrite() {
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  if (s->speed == 100 && s->servo != 0) {
    // move at regular/full 100% speed
    s->targetPos = ioCmd[2];
    s->currentPos = ioCmd[2];
    s->isMoving = false;
    s->servo->write(ioCmd[2]);
    if (s->eventsEnabled)
      sendServoEvent(s, SERVO_EVENT_STOPPED);
  } else if (s->speed < 100 && s->speed > 0) {
    s->targetPos = ioCmd[2];
    s->isMoving = true;
  }
}

/**
 * CONTROL METHODS END
 **********************************************************************/

/***********************************************************************
 * UPDATE DEVICES BEGIN
 * updateDevices updates each type of device put on the device list
 * depending on their type.
 * This method processes each loop. Typically this "back-end"
 * processing will read data from pins, or change states of non-blocking
 * pulses, or possibly regulate a motor based on pid values read from
 * pins
 */
void updateDevices() {
  // this is the update devices method..
  // iterate through our list of sensors
  for (int i = 0; i < deviceList.size(); i++) {
    Device* device = deviceList.get(i);
    switch (device->type) {
      case SENSOR_TYPE_ANALOG_PIN_ARRAY: {
    	  updateAnalogPinArray((MrlAnalogPinArray*)device);
        break;
      }
      case SENSOR_TYPE_DIGITAL_PIN_ARRAY: {
    	  updateDigitalPinArray((MrlDigitalPinArray*)device);
        break;
      }
      case SENSOR_TYPE_PULSE: {
        updatePulse((MrlPulse*)device);
        break;
      }
      case SENSOR_TYPE_ULTRASONIC: {
    	  updateUltrasonic((MrlUltrasonic*)device);
        break;
      }
      case DEVICE_TYPE_STEPPER: {
    	  updateStepper((MrlStepper*)device);
        break;
      }
      case DEVICE_TYPE_MOTOR: {
    	  updateMotor((MrlMotor*)device);
        break;
      }
      case DEVICE_TYPE_SERVO: {
    	  updateServo((MrlServo*)device);
        break;
      }
      case DEVICE_TYPE_I2C: {
    	  updateI2C((MrlI2CDevice*)device);
        break;
      }
      default:
        sendError(ERROR_UNKOWN_SENSOR);
        break;
    } // end switch(sensor.type)
  } // end for each pin
}

/***********************************************************************
 * UPDATE DEVICES END
 */

/**
 * This function updates the average time it took to run the main loop
 * and reports it back with a publishStatus MRLComm message
 */
void updateStatus() {
  // TODO: protect against a divide by zero
  unsigned long avgTiming = (micros() - lastMicros) / loadTimingModulus;
  // report load time
  if (loadTimingEnabled && (loopCount%loadTimingModulus == 0)) {
    // send the average loop timing.
    publishStatus(avgTiming, getFreeRam());
  }
}


// SERVO_ATTACH
void servoAttach() {
  // TODO: this is completely wrong, we need to create
  // a new device and return it's index.
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  s->index = ioCmd[1];
  if (s->servo == NULL) {
    s->servo = new Servo();
  }
  // Servo takes 1 pin
  s->servo->attach(ioCmd[2]);
  s->step = 1;
  s->eventsEnabled = false;
}



// PUBLISH_SERVO_EVENT
// FIXME - should be using PUBLISH_SENSOR_DATA
void publishServoEvent() {
  MrlServo* s = (MrlServo*)getDeviceByIndex(ioCmd[1]);
  s->eventsEnabled = ioCmd[2];
}


/***********************************************************************
 * ATTACH DEVICES BEGIN
 *
 * MSG STRUCTURE
 *        0           1            2          3     4    ....  N
 * ATTACH_DEVICE|DEVICE_TYPE|CONFIG_MSG_SIZE|DATA0|DATA1|....|DATA(N)
 *
 *
 * Device types are defined in org.myrobotlab.service.interface.Device
 * TODO crud Device operations create remove (update not needed?) delete
 * TODO probably need getDeviceId to decode id from Arduino.java - because if its
 * implemented as a ptr it will be 4 bytes - if it is a generics index
 * it could be implemented with 1 byte
 */

void attachDevice(){
  // we're creating a new device. auto increment it
  // TODO: consider what happens if we overflow on this auto-increment. (very unlikely. but possible)
  nextDeviceId++;
	int deviceType   = ioCmd[1];
	Device* devicePtr = 0;
	switch(deviceType){
	  case SENSOR_TYPE_ANALOG_PIN_ARRAY:{
		  devicePtr = attachAnalogPinArray();
		  break;
	  }
	  case SENSOR_TYPE_DIGITAL_PIN_ARRAY:{
		  devicePtr = attachDigitalPinArray();
		  break;
	  }
    case SENSOR_TYPE_PULSE: {
      devicePtr = attachPulse();
      break;
    }
    case SENSOR_TYPE_ULTRASONIC: {
      devicePtr = attachUltrasonic();
      break;
    }
    case DEVICE_TYPE_STEPPER: {
      devicePtr = attachStepper();
      break;
    }
    case DEVICE_TYPE_MOTOR: {
      devicePtr = attachMotor();
      break;
    }
    case DEVICE_TYPE_SERVO: {
      devicePtr = attachServo();
      break;
    }
    case DEVICE_TYPE_I2C: {
      devicePtr = attachI2C();
      break;
    }
	  default: {
	    // TODO: publish error message
	    break;
	  }
	}
	publishAttachedDevice(addDevice(devicePtr));
}

void detachDevice(int index) {
  // TODO: more effecient device detach/removal by walking the list
  // with a pointer
  int numDevices = deviceList.size();
  for (int i = 0; i < numDevices; i++) {
    if (deviceList.get(i)->index == index) {
      deviceList.remove(i);
      break;
    }
  }
}

Device* attachAnalogPinArray() {
  Device* pinArray = new MrlAnalogPinArray(nextDeviceId);
  // TODO: add the actual pins to this analog pin array.
  //deviceList.add(pinArray);
  return pinArray;
}

Device* attachDigitalPinArray() {
  Device* pinArray = new MrlDigitalPinArray(nextDeviceId);
  // set the device type to analog pin array
  // TODO: add the list of pins to the array <-- not necessary until a request to read/poll a pin is received
  // pinArray.pins
  // finally, add this to the current deviceList
  //deviceList.add(pinArray);
  return pinArray;
}

Device* attachServo() {
  // TODO: add a new servo device  (pontentally attach it here also?)
  //  int type    = ioCmd[1]; already here
  // TODO : check that we don't already have a servo attached to this pin
  int configSize     = ioCmd[2];
  // configSize "should" = 1
  int pinAddress       = ioCmd[3];
  // is this a copy constructor ?
  MrlServo* device = new MrlServo(pinAddress, nextDeviceId);
  return device;
}

Device* attachPulse(){
  MrlPulse* device = new MrlPulse(nextDeviceId);
  return device;
}

Device* attachUltrasonic(){
  MrlUltrasonic* device = new MrlUltrasonic(nextDeviceId);
  return device;
}

Device* attachStepper(){
  MrlStepper* device = new MrlStepper(nextDeviceId);
  return device;
}

Device* attachMotor(){
  MrlMotor* device = new MrlMotor(nextDeviceId);
  return device;
}

Device* attachI2C(){
  MrlI2CDevice* device = new MrlI2CDevice(nextDeviceId);
  return device;
}


// TODO: look if we should remove this method.
void addSensorDataListener() {
}
//  int sensorIndex    = ioCmd[1];
//  int sensorType     = ioCmd[2];
//  int pinCount       = ioCmd[3];
//  // TODO: the message should pass the list of pins that this sensor uses,
//  // and not assume sequential numbering!
//  // for loop grabbing all pins for this sensor
//  publishDebug("S_ATTACH: " + String(sensorIndex) + " type:" + String(sensorType) + " count:" + String(pinCount));
//  Device* s = new Device(DEVICE_TYPE_SERVO);
//  s->type = DEVICE_TYPE_SERVO;
//  s->index = sensorIndex;
//  LinkedList<Pin*> sensorPins = LinkedList<Pin*>();
//  // TODO: support an arbitrary list of pins being passed in
//  // right now, the pins are contigious
//  for (int ordinal = 0; ordinal < pinCount; ordinal++){
//    publishDebug("PINADD" + String(ordinal) + " TO " + String(pinCount));
//    // TODO: make sure it's the right pin type!
//    int type = SENSOR_TYPE_ANALOG_PIN_ARRAY;
//    Pin* sensorPin = new Pin(type, ordinal);
//    sensorPin->address = ordinal;
//    // TODO: rename this analog/digital ?
//    // sensorPin.deviceType = sensorType;
//    //sensorPin.isActive = true;
//    sensorPins.add(sensorPin);
//    // TODO: special considerations based on the type of sensor to
//    // setup the pins correctly for multi-pin sensors
//  }
//  publishDebug(F("adding pins."));
//  s->pins = sensorPins;
//  publishDebug(F("Adding sensors"));
//  deviceList.add(s);
//  publishDebug("NUM SENS:"+String(deviceList.size()));
//  publishDebug(F("Done with sensor attach."));
//}

// SENSOR_ATTACH
//void sensorAttach() {

  // TODO: replace this kahuna with the deviceAttach big kahuna!

  // THIS WILL BE THE NEW BIG-KAHUNA
  // INITIAL REQUEST - SENSOR GRABS ALL PINs IT NEEDS
  // IT THEN POPULATES each of the PINs with its sensorIndex
  // the uC (Arduino) - does not grab any - because it will
  // always take/recieve any non-reserved pin (softReset) Pin
  //publishDebug("BSAM");
  //int sensorIndex    = ioCmd[1];
  //int sensorType     = ioCmd[2];
  //int pinCount       = ioCmd[3];
  // for loop grabbing all pins for this sensor
  //for (int ordinal = 0; ordinal < pinCount; ordinal++){
    // grab the pin - assign the sensorIndex & sensorType
    //publishDebug("WHICH:" + String(ioCmd[4 + ordinal]));
    //Pin pin = Pin();
    //pin.sensorIndex = sensorIndex;
    //pin.sensorType = sensorType;
    //if (pin.sensorType == SENSOR_TYPE_ULTRASONIC && ordinal == 0) {
    //  publishDebug("ULTRAS");
      // pin.trigPin = ioCmd[3];
      // pin.echoPin = ioCmd[4];
    //  pinMode(pin.trigPin, OUTPUT); // WTF about wiring which has single pin ! :P
    //  pinMode(pin.echoPin, INPUT);
      //pin.ping = new NewPing(pin.trigPin, pin.echoPin, 100);
      // triggerPin's next pin is the echo pin
    //  pin.nextPin = ioCmd[5 + ordinal];
    //} else if (pin.sensorType == SENSOR_TYPE_PULSE) {
    //  publishDebug("PULSETYP");
    //  pin.address = ioCmd[3];
    //} else if (pin.sensorType == SENSOR_TYPE_PIN) {
      // TODO: ?!
    //  publishDebug("PINTYPE:" + String(ioCmd[1]) + " " + String(ioCmd[2]) + " " + String(ioCmd[3]) + " " + String(ioCmd[4]));
    //  pin.address = ioCmd[4];
      //pin.isActive = true;
      // we're reading form this pin now.
    //  pinMode(pin.address, INPUT);
    //} else {
    //  publishDebug("UNKNTYPE" + String(pin.sensorType));
    //}
 // }
 // publishDebug("ESAM");
//}

/**
 * ATTACH DEVICES END
**********************************************************************/


/***********************************************************************
 * PUBLISH DEVICES BEGIN
 * TODO should be publishError
 */

/**
 * send an error message/code back to MRL.
 * TODO - rename to publishError()
 */
void sendError(int type) {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // size = 1 FN + 1 TYPE
  Serial.write(PUBLISH_MRLCOMM_ERROR);
  Serial.write(type);
  Serial.flush();
}

/**
 * PUBLISH_ATTACHED_DEVICE
 * MSG STRUCTURE
 * PUBLISH_ATTACHED_DEVICE | NAME_STR_SIZE | NAME | NEW_DEVICE_INDEX
 *
 * FIXME - this needs work - got to be consistent on index
 * we had talked about using ptr - but now we are using index
 * of the linked list - which works(ish) until a remove is done
 *
 */
void publishAttachedDevice(int deviceIndex){
	Serial.write(MAGIC_NUMBER);
	Serial.write(2); // size
  // TODO:  figure how how to format this publish attached.
	Serial.write(PUBLISH_ATTACHED_DEVICE);
	int size = ioCmd[2];
	Serial.write(size);
  for (int i = 0; i  < size; i++) {
    Serial.write(ioCmd[i+2]);
  }
  Serial.write(deviceIndex);
	Serial.flush();
}


// publish a servo event.
void sendServoEvent(MrlServo* s, int eventType) {
  // check type of event - STOP vs CURRENT POS
  Serial.write(MAGIC_NUMBER);
  Serial.write(5); // size = 1 FN + 1 INDEX + 1 eventType + 1 curPos
  Serial.write(PUBLISH_SERVO_EVENT);
  Serial.write(s->index); // send my index
  // write the long value out
  Serial.write(eventType);
  Serial.write(s->currentPos);
  Serial.write(s->targetPos);
  Serial.flush();
}

void publishVersion() {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // size
  Serial.write(PUBLISH_VERSION);
  Serial.write((byte)MRLCOMM_VERSION);
  Serial.flush();
}


void publishStatus(unsigned long loadTime, int freeMemory) {
  Serial.write(MAGIC_NUMBER);
  Serial.write(7); // size 1 FN + 4 bytes of unsigned long
  Serial.write(PUBLISH_STATUS);

  // write the long value out
  Serial.write((byte)(loadTime >> 24));
  Serial.write((byte)(loadTime >> 16));
  Serial.write((byte)(loadTime >> 8));
  Serial.write((byte)loadTime & 0xff);

  // write the int value out
  Serial.write((byte)(freeMemory >> 8));
  Serial.write((byte)freeMemory & 0xff);

  Serial.flush();
}

void sendCommandAck() {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // size 1 FN + 1 bytes (the function that we're acking.)
  Serial.write(PUBLISH_MESSAGE_ACK);
  // the function that we're ack-ing
  Serial.write(ioCmd[0]);
  Serial.flush();
}

// This method will publish a string back to the Arduino service
// for debugging purproses.
// NOTE:  If this method gets called excessively
// I have seen memory corruption in the arduino where
// it seems to be getting a null string passed in as "message"
// very very very very very odd..  I suspect a bug in the arduino hard/software
void publishDebug(String message) {
  if (debug) {
    Serial.flush();
    Serial.write(MAGIC_NUMBER);
    Serial.write(1+message.length());
    Serial.write(PUBLISH_DEBUG);
    Serial.print(message);
    Serial.flush();
  }
}

// ================= publish methods end ==================

//========== device update methods begin ==================
//add pin.rateModulus, some sensor don't need to send back data every each ms
void updateAnalogPinArray(MrlAnalogPinArray* device) {
	if (device->pins.size() > 0) {
		Serial.write(MAGIC_NUMBER);
		Serial.write(2 + device->pins.size() * 2);
		Serial.write(PUBLISH_SENSOR_DATA);
		Serial.write(device->index);
		Serial.write(device->pins.size() * 2); // size of sensor data
		for (int i = 0; i < device->pins.size(); ++i) {
			Pin* pin = device->pins.get(i);
      // TODO: moe the analog read outside of thie method and pass it in!
			pin->value = analogRead(pin->address);
			Serial.write(pin->value >> 8);   // MSB
			Serial.write(pin->value & 0xff); // LSB
		}
		Serial.flush();
	}
}

//add pin.rateModulus, some sensor don't need to send back data every each ms
void updateDigitalPinArray(MrlDigitalPinArray* sensor) {
	if (sensor->pins.size() > 0) {
		Serial.write(MAGIC_NUMBER);
		Serial.write(2 + sensor->pins.size() * 1);
		Serial.write(PUBLISH_SENSOR_DATA);
		Serial.write(sensor->index);
		Serial.write(sensor->pins.size() * 1); // size of sensor data
		for (int i = 0; i < sensor->pins.size(); ++i) {
			Pin* pin = sensor->pins.get(i);
      // TODO: moe the digital read outside of thie method and pass it in!
			pin->value = digitalRead(pin->address);
			Serial.write(pin->value & 0xff); // LSB
		}
		Serial.flush();
	}
}

void updateUltrasonic(MrlUltrasonic* sensor) {
  Pin* pin = sensor->pins.get(0);
  if (pin->state == ECHO_STATE_START) {
    // trigPin prepare - start low for an
    // upcoming high pulse
    pinMode(sensor->trigPin, OUTPUT);
    digitalWrite(sensor->trigPin, LOW);
    // put the echopin into a high state
    // is this necessary ???
    pinMode(sensor->echoPin, OUTPUT);
    digitalWrite(sensor->echoPin, HIGH);
    unsigned long ts = micros();
    if (ts - sensor->ts > 2) {
      sensor->ts = ts;
      pin->state = ECHO_STATE_TRIG_PULSE_BEGIN;
    }
  } else if (pin->state == ECHO_STATE_TRIG_PULSE_BEGIN) {
    // begin high pulse for at least 10 us
    pinMode(sensor->trigPin, OUTPUT);
    digitalWrite(sensor->trigPin, HIGH);
    unsigned long ts = micros();
    if (ts - sensor->ts > 10) {
      sensor->ts = ts;
      pin->state = ECHO_STATE_TRIG_PULSE_END;
    }
  } else if (pin->state == ECHO_STATE_TRIG_PULSE_END) {
    // end of pulse
    pinMode(sensor->trigPin, OUTPUT);
    digitalWrite(sensor->trigPin, LOW);
    pin->state = ECHO_STATE_MIN_PAUSE_PRE_LISTENING;
    sensor->ts = micros();
  } else if (pin->state == ECHO_STATE_MIN_PAUSE_PRE_LISTENING) {
    unsigned long ts = micros();
    if (ts - sensor->ts > 1500) {
      sensor->ts = ts;
      // putting echo pin into listen mode
      pinMode(sensor->echoPin, OUTPUT);
      digitalWrite(sensor->echoPin, HIGH);
      pinMode(sensor->echoPin, INPUT);
      pin->state = ECHO_STATE_LISTENING;
    }
  } else if (pin->state == ECHO_STATE_LISTENING) {
    // timeout or change states..
    int value = digitalRead(sensor->echoPin);
    unsigned long ts = micros();
    if (value == LOW) {
      sensor->lastValue = ts - sensor->ts;
      sensor->ts = ts;
      pin->state = ECHO_STATE_GOOD_RANGE;
    } else if (ts - sensor->ts > sensor->timeoutUS) {
      pin->state = ECHO_STATE_TIMEOUT;
      sensor->ts = ts;
      sensor->lastValue = 0;
    }
  } else if (pin->state == ECHO_STATE_GOOD_RANGE || pin->state == ECHO_STATE_TIMEOUT) {
    // publishSensorDataLong(pin.address, sensor.lastValue);
    pin->state = ECHO_STATE_START;
  } // end else if
}

void updatePulse(MrlPulse* sensor) {
  sensor->lastValue = (sensor->lastValue == 0) ? 1 : 0;
  // leading edge ... 0 to 1
  Pin* pin = sensor->pins.get(0);
  if (sensor->lastValue == 1) {
    sensor->count++;
    if (pin->count >= pin->target) {
      pin->state = PUBLISH_PULSE_STOP;
    }
  }
  // change state of pin
  digitalWrite(pin->address, sensor->lastValue);
  // move counter/current position
  // see if feedback rate is valid
  // if time to send feedback do it
  // if (loopCount%feedbackRate == 0)
  // 0--to-->1 counting leading edge only
  // pin.method == PUBLISH_PULSE_PIN &&
  // stopped on the leading edge
  if (pin->state != PUBLISH_PULSE_STOP && sensor->lastValue == 1) {
    // TODO: support publish pulse stop!
    // publishPulseStop(pin.state, pin.sensorIndex, pin.address, sensor.count);
    // deactivate
    // lastDebounceTime[digitalReadPin[i]] = millis();
    // test git
  }
  if (pin->state == PUBLISH_PULSE_STOP) {
    //pin.isActive = false;
    // TODO: support publish pulse stop!
    // TODO: if we're not active, remove ourselves from the device list?
  }
  // publish the pulse!
  // TODO: support publishPuse!
  // publishPulse(pin.state, pin.sensorIndex, pin.address, pin.count);
}

void updateServo(MrlServo* servo) {
  // TODO: implement me. / test This seems to be just for sweeping stuffs? The first part is also use when Servo.speed!=100
  //It's possible that the servo never reach the targetPos if servo->step!=1
  if (servo->isMoving && servo->servo != 0) {
    if (servo->currentPos != servo->targetPos) {
      // caclulate the appropriate modulus to drive
      // the servo to the next position
      // TODO - check for speed > 0 && speed < 100 - send ERROR back?
      int speedModulus = (100 - servo->speed) * 10;
      // speed=99 will not be 99% of speed=100
      if (loopCount % speedModulus == 0) {
        int increment = servo->step * ((servo->currentPos < servo->targetPos) ? 1 : -1);
        // move the servo an increment
        servo->currentPos = servo->currentPos + increment;
        servo->servo->write(servo->currentPos);
        if (servo->eventsEnabled)
          sendServoEvent(servo, SERVO_EVENT_POSITION_UPDATE);
      }
    } else {
      if (servo->isSweeping) {
        if (servo->targetPos == servo->min) {
          servo->targetPos = servo->max;
        } else {
          servo->targetPos = servo->min;
        }
      } else {
        if (servo->eventsEnabled)
          sendServoEvent(servo, SERVO_EVENT_STOPPED);
        servo->isMoving = false;
      }
    }
  }
}

void updateStepper(MrlStepper* stepper) {
  // TODO: implement me
}

void updateMotor(MrlMotor* motor) {
  // TODO: implement me
}

void updateI2C(MrlI2CDevice* device) {
  // Nothing to implement for now
  // MRLComm acts as a master and a slave can never initiate
  // communication. It only responds to the master.
}
//========== device update methods end ==================
