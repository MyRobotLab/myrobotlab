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
  ListNode<T>* getNode(int id);

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
#define MRLCOMM_VERSION         37

// serial protocol functions
#define MAGIC_NUMBER            170 // 10101010

/**
* FIXME - first rule of generate club is: whole file should be generated
* so this needs to be turned itno a .h if necessary - but the manual munge
* should be replaced
* 
* Addendum up for vote:
*   Second rule of generate club is , to complete the mission, this file must/should go away...  
*   It should be generated, completely.  device subclasses, #defines and all..  muahahahhah! project mayhem...
* 
*   Third rule of generate club is, if something has not code and isn't used, remove it. If it has code, move the code.
* 
*/
// ----- MRLCOMM FUNCTION GENERATED INTERFACE BEGIN -----------
///// INO GENERATED DEFINITION BEGIN //////
// {publishMRLCommError Integer}
#define PUBLISH_MRLCOMM_ERROR		1
// {getVersion}
#define GET_VERSION		2
// {publishVersion Integer}
#define PUBLISH_VERSION		3
// {addSensorDataListener SensorDataListener int[]}
#define ADD_SENSOR_DATA_LISTENER		4
// {analogReadPollingStart Integer Integer}
#define ANALOG_READ_POLLING_START		5
// {analogReadPollingStop int}
#define ANALOG_READ_POLLING_STOP		6
// {analogWrite int int}
#define ANALOG_WRITE		7
// {attachDevice DeviceControl Object[]}
#define ATTACH_DEVICE		8
// {createI2cDevice int int String}
#define CREATE_I2C_DEVICE		9
// {detachDevice DeviceControl}
#define DETACH_DEVICE		10
// {digitalReadPollingStart Integer Integer}
#define DIGITAL_READ_POLLING_START		11
// {digitalReadPollingStop int}
#define DIGITAL_READ_POLLING_STOP		12
// {digitalWrite int int}
#define DIGITAL_WRITE		13
// {fixPinOffset Integer}
#define FIX_PIN_OFFSET		14
// {getBoardInfo}
#define GET_BOARD_INFO		15
// {i2cRead int int byte[] int}
#define I2C_READ		16
// {i2cWrite int int byte[] int}
#define I2C_WRITE		17
// {i2cWriteRead int int byte[] int byte[] int}
#define I2C_WRITE_READ		18
// {intsToString int[] int int}
#define INTS_TO_STRING		19
// {motorMove MotorControl}
#define MOTOR_MOVE		20
// {motorMoveTo MotorControl}
#define MOTOR_MOVE_TO		21
// {motorReset MotorControl}
#define MOTOR_RESET		22
// {motorStop MotorControl}
#define MOTOR_STOP		23
// {pinMode Integer Integer}
#define PIN_MODE		24
// {publishAttachedDevice DeviceControl}
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
// {releaseI2cDevice int int}
#define RELEASE_I2C_DEVICE		37
// {sensorPollingStart String}
#define SENSOR_POLLING_START		38
// {sensorPollingStop String}
#define SENSOR_POLLING_STOP		39
// {servoAttach Servo} - deleted method servoAttach, not device.attach
#define SERVO_ATTACH		40
// {servoDetach Servo}
#define SERVO_DETACH		41
// {servoEventsEnabled Servo boolean}
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


// TODO: Move these into ArduinoMsgCodec  and re-generate ...
//temporary #define This is duplicate defined as 70 and 15..
#define GET_BOARD_INFO 70
#define PUBLISH_BOARD_INFO 71
#define NEOPIXEL_WRITE_MATRIX 72
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
*   the DEVICE_TYPE_NOT_FOUND will manage the error instead of have it do random stuff
*/

// TODO: consider rename to DEVICE_TYPE_UNKNOWN ?  currently this isn't called anywhere
#define DEVICE_TYPE_NOT_FOUND           0

#define SENSOR_TYPE_ANALOG_PIN_ARRAY    1
#define SENSOR_TYPE_DIGITAL_PIN_ARRAY   2
#define SENSOR_TYPE_PULSE               3
#define SENSOR_TYPE_ULTRASONIC          4

#define DEVICE_TYPE_STEPPER             5
#define DEVICE_TYPE_MOTOR               6
#define DEVICE_TYPE_SERVO               7
#define DEVICE_TYPE_I2C                 8
#define DEVICE_TYPE_NEOPIXEL            9

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
#define BOARD_TYPE_ID_UNKNOWN 0
#define BOARD_TYPE_ID_MEGA    1
#define BOARD_TYPE_ID_UNO     2

#if defined(ARDUINO_AVR_MEGA2560)
  #define BOARD BOARD_TYPE_ID_MEGA
#elif defined(ARDUINO_AVR_UNO)
  #define BOARD BOARD_TYPE_ID_UNO
#else
  #define BOARD BOARD_TYPE_ID_UNKNOWN
#endif

/***********************************************************************
 * NEOPIXEL DEFINE
 */
// PORTC BIT7 = pin 30
#define PIXEL_PORT PORTC
#define PIXEL_DDR DDRC
//timing for the neopixel communication
#define T1H 900    
#define T1L 600
#define T0H 400
#define T0L 900
#define RES 6000

#define NS_PER_SEC (1000000000L)
#define CYCLES_PER_SEC (F_CPU)
#define NS_PER_CYCLE ( NS_PER_SEC / CYCLES_PER_SEC )
#define NS_TO_CYCLES(n) ( (n) / NS_PER_CYCLE )

void publishError(int type, String message);

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
    // remove me, needed for pulse
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
    // TODO: consider making this Device(int deviceType, int[] config) so the device always has a handle to it's original config.
    Device(int deviceType) {
      type = deviceType;
    }
    ~Device(){
      // default destructor for the device class. 
    }
    int id; // the all important id of the sensor - equivalent to the "name" - used in callbacks
    int type; // what type of device is this?
    int state; // state - single at the moment to handle all the finite states of the sensors (todo maybe this moves into the subclasses?)
    virtual void update(unsigned long loopCount) {}; // all devices must implement this to update their state.
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
    int step;
    int min;
    int max;

    MrlServo(int p) : Device(DEVICE_TYPE_SERVO) {
       pin = p;
       isMoving = false;
       isSweeping = false;
       speed = 100;// 100% speed
       // TODO: target/curent position?
       // create the servo
       servo = new Servo();
       eventsEnabled = false;
    }

    ~MrlServo() {
      servo->detach();
      delete servo;
    }

    void attach(int pin){
    	servo->attach(pin);
    	// TODO-KW: we should always have a moveTo for safety, o/w we have no idea what angle we're going to start up at.. maybe
    }

    void update(unsigned long loopCount) {
      // TODO: implement me. / test This seems to be just for sweeping stuffs? The first part is also use when Servo.speed!=100
      //It's possible that the servo never reach the targetPos if servo->step!=1
      if (isMoving && servo != 0) {
        if (currentPos != targetPos) {
          // caclulate the appropriate modulus to drive
          // the servo to the next position
          // TODO - check for speed > 0 && speed < 100 - send ERROR back?
          int speedModulus = (100 - speed) * 10;
          // speed=99 will not be 99% of speed=100
          if (loopCount % speedModulus == 0) {
            int increment = step * ((currentPos < targetPos) ? 1 : -1);
            // move the servo an increment
            currentPos = currentPos + increment;
            servo->write(currentPos);
            if (eventsEnabled)
              publishServoEvent(SERVO_EVENT_POSITION_UPDATE);
          }
        } else {
          if (isSweeping) {
            if (targetPos == min) {
              targetPos = max;
            } else {
              targetPos = min;
            }
          } else {
            if (eventsEnabled)
              publishServoEvent(SERVO_EVENT_STOPPED);
            isMoving = false;
          }
        }
      }
    }
    
    // TODO: consider moving all Serial.write stuff out of device classes! -KW
    // I don't want devices knowing about the serial port directly.
    // consider a lifecycle where a device yeilds a message  to publish perhaps.
    void publishServoEvent(int eventType) {
      Serial.write(MAGIC_NUMBER);
      Serial.write(5); // size = 1 FN + 1 INDEX + 1 eventType + 1 curPos
      Serial.write(PUBLISH_SERVO_EVENT);
      Serial.write(id); // send my id
      // write the long value out
      Serial.write(eventType);
      Serial.write(currentPos);
      Serial.write(targetPos);
      Serial.flush();
    }
    
    void servoEventEnabled(int value) {
      eventsEnabled=value;
    }
    
    void servoWrite(int position) {
      if (speed == 100 && servo != 0) {
        // move at regular/full 100% speed
        targetPos = position;
        currentPos = position;
        isMoving = false;
        // servo->attach(8);
        servo->write(position);
        if (eventsEnabled)
          publishServoEvent(SERVO_EVENT_STOPPED);
      } else if (speed < 100 && speed > 0) {
        targetPos = position;
        isMoving = true;
      }
    }

    void servoWriteMicroseconds(int position) {
      if (servo) {
        servo->writeMicroseconds(position);
      }
    }

    void setSpeed(int speed) {
      this->speed = speed;
    }
    
    void startSweep(int min, int max, int step) {
      this->min = min;
      this->max = max;
      this->step = step;
      isMoving = true;
      isSweeping = true;
    }
    
    void stopSweep() {
      isMoving = false;
      isSweeping = false;
    }

};

/**
 * Motor Device
 */
class MrlMotor : public Device {
  // details of the different motor controls/types
  public:
    MrlMotor() : Device(DEVICE_TYPE_MOTOR) {
      // TODO: implement classes or custom control for different motor controller types
      // usually they require 2 PWM pins, direction & speed...  sometimes the logic is a
      // little different to drive it.
    }
    void update(unsigned long loopCount) {
      // we should update the pwm values for the control of the motor device  here
      // this is potentially where the hardware specific logic could go for various motor controllers
      // L298N vs IBT2 vs other...  maybe consider a subclass for the type of motor-controller.
    }
};

/**
 * Stepper Device
 */
class MrlStepper : public Device {
  // details of the different motor controls/types
  public:
    MrlStepper() : Device(DEVICE_TYPE_STEPPER) {

    }
    void update(unsigned long loopCount) {
    }
};

/**
 * Analog Pin Array Sensor 
 * This represents a set of pins that can be sampled
 */
class MrlAnalogPinArray : public Device {
  // int pins[];
  public:
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    MrlAnalogPinArray() : Device(SENSOR_TYPE_ANALOG_PIN_ARRAY) {
  // specific stuffs for analog pins (sample rates?)
    }
    ~MrlAnalogPinArray() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    // TODO: remove the direct write to the serial here..  devices shouldn't 
    // have a direct handle to the serial port.
    void update(unsigned long loopCount) {
      if (pins.size() > 0) {
        Serial.write(MAGIC_NUMBER);
        Serial.write(2 + pins.size() * 2);
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(id);
        Serial.write(pins.size() * 2); // size of sensor data
        for (int i = 0; i < pins.size(); ++i) {
          Pin* pin = pins.get(i);
          // TODO: moe the analog read outside of thie method and pass it in!
          pin->value = analogRead(pin->address);
          Serial.write(pin->value >> 8);   // MSB
          Serial.write(pin->value & 0xff); // LSB
        }
        Serial.flush();
      }
    }
};

/**
 * Digital Pin Array Sensor
 */
class MrlDigitalPinArray : public Device {
  public:
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    // config such as if they should publish the value in every loop
    // or only on a state change.
    MrlDigitalPinArray() : Device(SENSOR_TYPE_DIGITAL_PIN_ARRAY) {
    //pins = sensorPins;
    //for (int i = 0; i < pins.size(); i++) {
      // TODO: is this Analog or Digital?
    //  pinMode(i, INPUT);
    //}
    }
    ~MrlDigitalPinArray() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    // devices shouldn't have a direct handle to the serial port..
    void update(unsigned long loopCount) {
      if (pins.size() > 0) {
        Serial.write(MAGIC_NUMBER);
        Serial.write(2 + pins.size() * 1);
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(id);
        Serial.write(pins.size() * 1); // size of sensor data
        for (int i = 0; i < pins.size(); ++i) {
          Pin* pin = pins.get(i);
          // TODO: moe the digital read outside of thie method and pass it in!
          pin->value = digitalRead(pin->address);
          Serial.write(pin->value & 0xff); // LSB
        }
        Serial.flush();
      }
    }
};

/**
 * Pulse Sensor Device
 */
class MrlPulse : public Device {
  public:
    unsigned long lastValue;
    int count;
    Pin* pin;
    MrlPulse() : Device(SENSOR_TYPE_PULSE) {
      count=0;
    }
    ~MrlPulse() {
      if (pin) delete pin;
    }
    void pulse(unsigned char* ioCmd) {
      if (!pin) return;
      pin->count = 0;
      pin->target = toUnsignedLongfromBigEndian(ioCmd,2);
      pin->rate = ioCmd[6];
      pin->rateModulus = ioCmd[7];
      pin->state = PUBLISH_SENSOR_DATA;
    }
    void pulseStop() {
      pin->state = PUBLISH_PULSE_STOP;
    }
    void update(unsigned long loopCount) {
      //this need work
      if (type == 0) return;
      lastValue = (lastValue == 0) ? 1 : 0;
      // leading edge ... 0 to 1
      if (lastValue == 1) {
        count++;
        if (pin->count >= pin->target) {
          pin->state = PUBLISH_PULSE_STOP;
        }
      }
      // change state of pin
      digitalWrite(pin->address, lastValue);
      // move counter/current position
      // see if feedback rate is valid
      // if time to send feedback do it
      // if (loopCount%feedbackRate == 0)
      // 0--to-->1 counting leading edge only
      // pin.method == PUBLISH_PULSE_PIN &&
      // stopped on the leading edge
      if (pin->state != PUBLISH_PULSE_STOP && lastValue == 1) {
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
    unsigned long toUnsignedLongfromBigEndian(unsigned char* buffer, int start) {
      return (((unsigned long)buffer[start] << 24) +
              ((unsigned long)buffer[start + 1] << 16) +
              (buffer[start + 2] << 8) + buffer[start + 3]);
    }
};

/**
 * Ultrasonic Sensor
 */
class MrlUltrasonic : public Device {
  public:
    int trigPin;
    int echoPin;
    LinkedList<Pin*> pins; // the pins currently assigned to this sensor 0
    unsigned long ts;
    unsigned long lastValue;
    int timeoutUS;
    MrlUltrasonic() : Device(SENSOR_TYPE_ULTRASONIC) {
      timeoutUS=0; //this need to be set
      trigPin=0;//this need to be set
      echoPin=0;//this need to be set
    }
    ~MrlUltrasonic() {
      while(pins.size()>0){
        delete pins.pop();
      }

    }
    void update(unsigned long loopCount) {
      //This need to be reworked
      if (type == 0) return;
      Pin* pin = pins.get(0);
      if (pin->state == ECHO_STATE_START) {
        // trigPin prepare - start low for an
        // upcoming high pulse
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, LOW);
        // put the echopin into a high state
        // is this necessary ???
        pinMode(echoPin, OUTPUT);
        digitalWrite(echoPin, HIGH);
        unsigned long newts = micros();
        if (newts - ts > 2) {
          ts = newts;
          pin->state = ECHO_STATE_TRIG_PULSE_BEGIN;
        }
      } else if (pin->state == ECHO_STATE_TRIG_PULSE_BEGIN) {
        // begin high pulse for at least 10 us
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, HIGH);
        unsigned long newts = micros();
        if (newts - ts > 10) {
          ts = newts;
          pin->state = ECHO_STATE_TRIG_PULSE_END;
        }
      } else if (pin->state == ECHO_STATE_TRIG_PULSE_END) {
        // end of pulse
        pinMode(trigPin, OUTPUT);
        digitalWrite(trigPin, LOW);
        pin->state = ECHO_STATE_MIN_PAUSE_PRE_LISTENING;
        ts = micros();
      } else if (pin->state == ECHO_STATE_MIN_PAUSE_PRE_LISTENING) {
        unsigned long newts = micros();
        if (newts - ts > 1500) {
          ts = newts;
          // putting echo pin into listen mode
          pinMode(echoPin, OUTPUT);
          digitalWrite(echoPin, HIGH);
          pinMode(echoPin, INPUT);
          pin->state = ECHO_STATE_LISTENING;
        }
      } else if (pin->state == ECHO_STATE_LISTENING) {
        // timeout or change states..
        int value = digitalRead(echoPin);
        unsigned long newts = micros();
        if (value == LOW) {
          lastValue = newts - ts;
          ts = newts;
          pin->state = ECHO_STATE_GOOD_RANGE;
        } else if (newts - ts > timeoutUS) {
          pin->state = ECHO_STATE_TIMEOUT;
          ts = newts;
          lastValue = 0;
        }
      } else if (pin->state == ECHO_STATE_GOOD_RANGE || pin->state == ECHO_STATE_TIMEOUT) {
        // publishSensorDataLong(pin.address, sensor.lastValue);
        pin->state = ECHO_STATE_START;
      } // end else if
    }
};

/**
 * I2C device
 */
class MrlI2CDevice : public Device {
  public:
    MrlI2CDevice() : Device(DEVICE_TYPE_I2C) {
      if (TWCR == 0) { //// do this check so that Wire only gets initialized once
        WIRE.begin();
      }
    }

    // I2CREAD | DEVICE_INDEX | I2CADDRESS | DATA_SIZE
    // PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATA_SIZE | I2CADDRESS | DATA ....
    // DEVICE_INDEX = Index to the I2C bus
    // I2CDEVICEINDEX = Index used by Arduino to return the read data to the device
    // that requested a read
    void i2cRead(unsigned char* ioCmd) {

      int answer = WIRE.requestFrom((uint8_t)ioCmd[3], (uint8_t)ioCmd[4]); // reqest a number of bytes to read
      if (answer==0) {
        //Report an error with I2C communication by returning a 1 length data size //i.e a message size if 1 byte containing only PUBLISH_SENSOR_DATA
        // Start an mrlcomm message
        Serial.write(MAGIC_NUMBER);
        // size of the mrlcomm message
        Serial.write(4);
        // mrlcomm function
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(ioCmd[1]); // DEVICE_INDEX
        Serial.write(1);        // DATA_SIZE ( 1 byte for the i2caddress )
        Serial.write(ioCmd[3]); // I2CADDRESS
        } else {
        // Start an mrlcomm message
        Serial.write(MAGIC_NUMBER);
        // size of the mrlcomm message
        Serial.write(1 + ioCmd[4]);
        // mrlcomm function
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(ioCmd[1]); // DEVICE_INDEX
        Serial.write(1+answer); // DATA_SIZE ( add 1 byte for the i2caddress )
        Serial.write(ioCmd[3]); // I2CADDRESS
        for (int i = 1; i<answer; i++) {
          Serial.write(Wire.read());
        }
      }
    }

    void i2cWrite(unsigned char* ioCmd) {
    	int msgSize = ioCmd[3];
        WIRE.beginTransmission(ioCmd[1]);   // address to the i2c device
        WIRE.write(ioCmd[2]);               // device memory address to write to
        for (int i = 3; i < msgSize; i++) { // data to write
          WIRE.write(ioCmd[i]);
        }
        WIRE.endTransmission();
    }
    
    // I2WRITEREAD | DEVICE_INDEX | DATA_SIZE | I2CADDRESS | DEVICE_MEMORY_ADDRESS | DATA.....
    // PUBLISH_SENSOR_DATA | DEVICE_INDEX | DATA_SIZE | I2CADDRESS | DATA ....
    // DEVICE_INDEX = Index to the I2C bus
    // I2CDEVICEINDEX = Index used by Arduino to return the read data to the device
    // that requested a read
    void i2cWriteRead(unsigned char* ioCmd) {
      WIRE.beginTransmission(ioCmd[4]); // address to the i2c device
      WIRE.write(ioCmd[5]);             // device memory address to read from
      WIRE.endTransmission();
      int answer = WIRE.requestFrom((uint8_t)ioCmd[2], (uint8_t)ioCmd[4]); // reqest a number of bytes to read
      if (answer==0) {
        //Report an error with I2C communication by returning a 1 length data size //i.e a message size if 1 byte containing only PUBLISH_SENSOR_DATA
        // Start an mrlcomm message
        Serial.write(MAGIC_NUMBER);
        // size of the mrlcomm message
        Serial.write(4);
        // mrlcomm function
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(ioCmd[1]); // DEVICE_INDEX
        Serial.write(1);        // DATA_SIZE ( 1 byte for the i2caddress )
        Serial.write(ioCmd[3]); // I2CADDRESS
        } else {
        // Start an mrlcomm message
        Serial.write(MAGIC_NUMBER);
        // size of the mrlcomm message
        Serial.write(1 + ioCmd[4]);
        // mrlcomm function
        Serial.write(PUBLISH_SENSOR_DATA);
        Serial.write(ioCmd[1]); // DEVICE_INDEX
        Serial.write(1+answer); // DATA_SIZE ( add 1 byte for the i2caddress )
        Serial.write(ioCmd[3]); // I2CADDRESS
        for (int i = 1; i<answer; i++) {
          Serial.write(Wire.read());
        }
      }
    }
    void update(unsigned long loopCount) {
      //Nothing to do
    }
};

/*****************************
 * Neopixel device
 * 
 * adapted from https://github.com/bigjosh/SimpleNeoPixelDemo/blob/master/SimpleNeopixelDemo/SimpleNeopixelDemo.ino
 * it contains board specific code
 * so far only working on pins 30-37 on Mega
 * TODO: support on more pins and on UNO
 */
struct Pixel{
  unsigned char red;
  unsigned char blue;
  unsigned char green;
  Pixel(){
    red=0;
    blue=0;
    green=0;
  }
};

class MrlNeopixel:public Device{
  public:
  unsigned int numPixel;
  Pixel* pixels;
  uint8_t bitmask;
  unsigned long lastShow;
  bool newData;
  MrlNeopixel(int pin, unsigned int num_pixel):Device(DEVICE_TYPE_NEOPIXEL){
    numPixel = num_pixel;
    pixels = new Pixel[numPixel+1];
    if(BOARD==BOARD_TYPE_ID_UNKNOWN) {
      publishError(ERROR_DOES_NOT_EXIST,F("Board not supported"));
      state=0;
    }
    if(pin<30 || pin>37) {
      publishError(ERROR_DOES_NOT_EXIST,F("Pin not supported"));
      state=0;
    }
    else{
      state=1;
      bitmask=digitalPinToBitMask(pin);
    }
    PIXEL_DDR |= bitmask;
    lastShow=0;
    Pixel pixel=Pixel();
    for (int i=1; i<=numPixel; i++) {
      pixels[i] = pixel;
    }
    newData=true;
  }
  ~MrlNeopixel(){
    delete pixels;
  }
 inline void sendBit(bool bitVal){
    uint8_t bit=bitmask;
    if (bitVal) {        // 0 bit
      PIXEL_PORT |= bit;
      asm volatile (
        ".rept %[onCycles] \n\t"                                // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [onCycles]  "I" (NS_TO_CYCLES(T1H) - 2)    // 1-bit width less overhead  for the actual bit setting, note that this delay could be longer and everything would still work
        );
      PIXEL_PORT &= ~bit;
      asm volatile (
        ".rept %[offCycles] \n\t"                               // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [offCycles]   "I" (NS_TO_CYCLES(T1L) - 2)     // Minimum interbit delay. Note that we probably don't need this at all since the loop overhead will be enough, but here for correctness
      );
    } else {          // 1 bit
      // **************************************************************************
      // This line is really the only tight goldilocks timing in the whole program!
      // **************************************************************************
      cli(); //desactivate interrupts
      PIXEL_PORT |= bit ;
      asm volatile (
        ".rept %[onCycles] \n\t"        // Now timing actually matters. The 0-bit must be long enough to be detected but not too long or it will be a 1-bit
        "nop \n\t"                                              // Execute NOPs to delay exactly the specified number of cycles
        ".endr \n\t"
        ::
        [onCycles]  "I" (NS_TO_CYCLES(T0H) - 2)
        );
      PIXEL_PORT &= ~bit;
      asm volatile (
        ".rept %[offCycles] \n\t"                               // Execute NOPs to delay exactly the specified number of cycles
        "nop \n\t"
        ".endr \n\t"
        ::
        [offCycles] "I" (NS_TO_CYCLES(T0L) - 2)
      );
      sei(); //activate interrupts
    }
      // Note that the inter-bit gap can be as long as you want as long as it doesn't exceed the 5us reset timeout (which is A long time)
      // Here I have been generous and not tried to squeeze the gap tight but instead erred on the side of lots of extra time.
      // This has thenice side effect of avoid glitches on very long strings becuase 
  }
    
  inline void sendByte(unsigned char byte) {
    for(unsigned char bit = 0 ; bit < 8 ; bit++ ) {
      sendBit( bitRead( byte , 7 ) );                // Neopixel wants bit in highest-to-lowest order
                                                     // so send highest bit (bit #7 in an 8-bit byte since they start at 0)
      byte <<= 1;                                    // and then shift left so bit 6 moves into 7, 5 moves into 6, etc
    }           
  }
  inline void sendPixel(Pixel p) {  
    sendByte(p.green);          // Neopixel wants colors in green then red then blue order
    sendByte(p.red);
    sendByte(p.blue);
  }
  void show(){
    if (!state) return;
    //be sure we wait at least 6us before sending new data
    if ((lastShow+(RES/1000UL))>micros()) return;
    for(int p=1; p<=numPixel;p++){
      sendPixel(pixels[p]);
    }
    lastShow=micros();
    newData=false;

  }
  void neopixelWriteMatrix(unsigned char* ioCmd) {
    for (int i=3; i<ioCmd[2]+3;i+=4){
      pixels[ioCmd[i]].red=ioCmd[i+1];
      pixels[ioCmd[i]].green=ioCmd[i+2];
      pixels[ioCmd[i]].blue=ioCmd[i+3];
    }
    newData=true;
  }
  void update(unsigned long loopCount){
    if((lastShow+33000)>micros() || !newData) return; //update 30 times/sec if there is new data to show
    show();
  }
};


/***********************************************************************
 * GLOBAL VARIABLES
 * TODO - work on reducing globals and pass as parameters
*/

// The mighty device List.  This contains all active devices that are attached to the arduino.
LinkedList<Device*> deviceList;
unsigned int nextDeviceId = 0;

// MRLComm message buffer and current count from serial port ( MAGIC | MSGSIZE | FUNCTION | PAYLOAD ...
int msgSize = 0; // the NUM_BYTES of current message (second byte of mrlcomm protocol)

unsigned char ioCmd[MAX_MSG_SIZE];  // message buffer for all inbound messages

// N.B. - not sure if its should be signed or unsigned - the data
// for this originates from ioCmd
// KW, i think this should move to the constuctor of the device classes.  devices should have a handle to their configs.
// KW : i just don't like this being global..it's a lot of state to keep global.. not to mention it's a copy of what's in ioCmd 
unsigned char config[MAX_MSG_SIZE];  // config buffer
int configPos = 0; // position of the beginning of config in the ioCmd message

int byteCount = 0;
unsigned long loopCount = 0; // main loop count
// TODO: this is referenced nowhere. remove? 
unsigned int debounceDelay = 50; // in ms
// performance metrics  and load timing
bool loadTimingEnabled = false;
int loadTimingModulus = 1000; // the frequency in which to report the load timing metrics (in number of main loops)
unsigned long lastMicros = 0; // timestamp of last loop (if stats enabled.)

// sensor sample rate
unsigned int sampleRate = 1; // 1 - 65,535 modulus of the loopcount - allowing you to sample less

// global debug setting, if set to true publishDebug will write to the serial port.
bool debug = false;

/***********************************************************************
 * DEVICE LIST ACCESS METHODS BEGIN
 * basic crud operations for devices to seperate the implementation
 * details of the data structure containing all the devices
 */
Device* getDevice(int id);
void removeDevice(int id);
int addDevice(Device*);
/**
 * DEVICE LIST ACCESS METHODS END
 **********************************************************************/
void publishDebug(String message);

/***********************************************************************
 * STANDARD ARDUINO BEGIN 
 * setup() is called when the serial port is opened unless you hack the 
 * serial port on your arduino
 * 
 * Here we default out serial port to 115.2kbps.
*/
void setup() {
  Serial.begin(115200);        // connect to the serial port
  while (!Serial){};
  // TODO: the arduino service might get a few garbage bytes before we're able
  // to run, we should consider some additional logic here like a "publishReset"
  softReset();
  // publish version on startup so it's immediately available for mrl.
  // TODO: see if we can purge the current serial port buffers
  publishVersion();
  // publish the board type (uno/mega)
  publishBoardInfo();
}

/**
 * Arduino loop() method. 
 * This method will be called over and over again by the arduino
 */
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

int getFreeRam() {
  // KW: In the future the arduino might have more than an 32/64k of ram. an int might not be enough here to return.
  extern int __heap_start, *__brkval;
  int v;
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
}

void softReset() {
  // TODO: do this before we start the serial port? At this point the memory 
  // space should be clean, but we don't know.. always safe to sweep the memory 
  // that we intend to use.
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
 * This adds a device to the current set of active devices in the deviceList.
 * 
 * FIXME - G: I think dynamic array would work better
 * at least for the deviceList 
 * TODO: KW: i think it's pretty dynamic now.
 */
int addDevice(Device* device) {
  deviceList.add(device);
}

/**
 * getDevice - this method will look up a device by it's id in the device list.
 * it returns null if the device isn't found.
 */
Device* getDevice(int id) {
  // TODO: more effecient lookup..use a linked list iterator here..  while (nextNode != null) sort of iteration..
  int numDevices = deviceList.size();
  for (int i = 0; i < numDevices; i++) {
    Device* dev=deviceList.get(i);
    if (dev->id == id) {
      // it was found.
      return dev;
    }
  }
  // TODO: force this to return an error message
  publishError(ERROR_DOES_NOT_EXIST);
  return 0; //returning a NULL ptr can cause runtime error
  // you'll still get a runtime error if any field, member or method not
  // defined is accessed
}

/**
 * UTILITY METHODS END
 ***********************************************************************/

/***********************************************************************
 * SERIAL METHODS BEGIN
 */
 
/**
 * getCommand() - This is the main method to read new data from the serial port, 
 * when a full mrlcomm message is read from the serial port.
 * return values: true if the serial port read a full mrlcomm command
 *                false if the serial port is still waiting on a command.
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
        publishError(ERROR_SERIAL);
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
/**
 * processCommand() - once the main loop has read an mrlcomm message from the 
 * serial port, this method will be called.
 */
 // TODO: change this to something liek processCommand(MrlCommand cmd) ...
 // passing around ioCmd as global is messy!
void processCommand() {
  // TODO-KW: make this switch statement go away.. or move somewhere else..
  // it'd be nice to have command.process(ioCmd[])  ..
  switch (ioCmd[0]) {
    // === system pass through begin ===
    case DIGITAL_WRITE:
      digitalWrite(ioCmd[1], ioCmd[2]);
      break;
    case ANALOG_WRITE:{
      analogWrite(ioCmd[1], ioCmd[2]);
      break;
    }
    case PIN_MODE:{
      pinMode(ioCmd[1], ioCmd[2]);
      break;
    }
    case SERVO_ATTACH:{
      // Servo.attach(pin)
	    int pin = ioCmd[2];
	    publishDebug("SERVO_ATTACH " + String(pin));
  	  MrlServo* s = (MrlServo*)getDevice(ioCmd[1]);
	    s->attach(pin);
  	  publishDebug("SERVO_ATTACHED ");
      break;
    }
    case SERVO_SWEEP_START:
      //startSweep(min,max,step)
      ((MrlServo*)getDevice(ioCmd[1]))->startSweep(ioCmd[2],ioCmd[3],ioCmd[4]);
      break;
    case SERVO_SWEEP_STOP:
      ((MrlServo*)getDevice(ioCmd[1]))->stopSweep();
      break;
    case SERVO_EVENTS_ENABLED:
      // PUBLISH_SERVO_EVENT seem to do the same thing
      servoEventsEnabled();
      break;
    case SERVO_WRITE:
      ((MrlServo*)getDevice(ioCmd[1]))->servoWrite(ioCmd[2]);
      break;
    case PUBLISH_SERVO_EVENT:
      ((MrlServo*)getDevice(ioCmd[1]))->servoEventEnabled(ioCmd[2]);
      break;
    case SERVO_WRITE_MICROSECONDS:
      ((MrlServo*)getDevice(ioCmd[1]))->servoWriteMicroseconds(ioCmd[2]);
      break;
    case SET_SERVO_SPEED:
      ((MrlServo*)getDevice(ioCmd[1]))->setSpeed(ioCmd[2]);
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
      break;
    case PULSE:
      ((MrlPulse*)getDevice(ioCmd[1]))->pulse(ioCmd);
      break;
    case PULSE_STOP:
      ((MrlPulse*)getDevice(ioCmd[1]))->pulseStop();
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
      publishVersion();
      break;
    case SET_SAMPLE_RATE:
      setSampleRate();
      break;
    case SOFT_RESET:
      softReset();
      break;
    case ADD_SENSOR_DATA_LISTENER:
      // TODO: replace this with simple attachDevice call.
      // addSensorDataListener();
      break;
    case SENSOR_POLLING_START:
      sensorPollingStart();
      break;
    case ATTACH_DEVICE:
	    attachDevice();
	    break;
    case SENSOR_POLLING_STOP:
      sensorPollingStop();
      break;
    // Start of i2c read and writes
    case I2C_READ:
      ((MrlI2CDevice*)getDevice(ioCmd[1]))->i2cRead(&ioCmd[0]);
      break;
    case I2C_WRITE:
      ((MrlI2CDevice*)getDevice(ioCmd[1]))->i2cWrite(&ioCmd[0]);
      break;
    case I2C_WRITE_READ:
      ((MrlI2CDevice*)getDevice(ioCmd[1]))->i2cWriteRead(&ioCmd[0]);
      break;
    case SET_DEBUG:
      debug = ioCmd[1];
      if (debug) {
        publishDebug(F("Debug logging enabled."));
      }
      break;
    case GET_BOARD_INFO:
      publishBoardInfo();
      break;
    case NEOPIXEL_WRITE_MATRIX:
      ((MrlNeopixel*)getDevice(ioCmd[1]))->neopixelWriteMatrix(ioCmd);
      break;
    default:
      publishError(ERROR_UNKOWN_CMD);
      break;
  } // end switch
  // ack that we got a command (should we ack it first? or after we process the command?)
  publishCommandAck();
  // reset command buffer to be ready to receive the next command.
  // KW: we should only need to set the byteCount back to zero. clearing this array is just for safety sake i guess?
  memset(ioCmd, 0, sizeof(ioCmd));
  byteCount = 0;
} // process Command

/**
 * SERIAL METHODS END
 **********************************************************************/

/***********************************************************************
 * CONTROL METHODS BEGIN
 * These methods map one to one for each MRLComm command that comes in.
 * 
 * TODO - add text api
 */

void sensorPollingStart() {
  // TODO: implement me.
}

void sensorPollingStop() {
  // TODO: implement me.
}

// SERVO_DETACH
void servoDetach() {
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

// ANALOG_READ_POLLING_START
void analogReadPollingStart() {
  // TODO: remove this method.. potentially replace with device attach.
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

// digital_READ_POLLING_STOP
void digitalReadPollingStop() {
  //Device* d = getDevice(ioCmd[1]);
  //Pin* pin = d->pins.get(0);
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
  // consider removing me?  or move it to a digital pin device or something?
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
  // TODO: move this method to the device classes that have a sample rate.
  // 2 byte int - valid range 1-65,535
  sampleRate = (ioCmd[1] << 8) + ioCmd[2];
  if (sampleRate == 0) {
    sampleRate = 1;
  } // avoid /0 error - FIXME - time estimate param
}

// SERVO_EVENTS_ENABLED
void servoEventsEnabled() {
  // Not implemented.
  // TODO: move this to the servo device class.
  // or just remove it all together.
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
    deviceList.get(i)->update(loopCount);
  } // end for each device
}

/***********************************************************************
 * UPDATE DEVICES END
 */

/**
 * UPDATE STATUS 
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
void attachDevice() {
  // TOOD:KW check free memory to see if we can attach a new device. o/w return an error!
	// we're creating a new device. auto increment it
	// TODO: consider what happens if we overflow on this auto-increment. (very unlikely. but possible)
	//       Arduino will run out of memory before that happen.
	//       A mecanism that will call a softReset() when MRL disconnect will prevent that
	// we want to echo back the name
	// and send the config in a nice neat package to
	// the attach method which creates the device

	int nameSize = ioCmd[2];

	// get config size
	int configSizePos = 3 + nameSize;
	int configSize = ioCmd[configSizePos];
	configPos = configSizePos + 1;

	// MAKE NOTE: I've chosen to have config & configPos globals
	// this is primarily to avoid the re-allocation/de-allocation of the config buffer
	// but part of me thinks it should be a local var passed into the function to avoid
	// the dangers of global var ... fortunately Arduino is single threaded
	// It also makes sense to pass in config on the constructor of a new device
	// based on device type - "you inflate the correct device with the correct config"
	// but I went on the side of globals & hopefully avoiding more memory management and fragmentation

	// move config off ioCmd into config buffer
	for (int i = 0; i < configSize; ++i){
		config[i] = ioCmd[configPos + i];
	}

	int deviceType = ioCmd[1];
	Device* devicePtr = 0;
	// KW: remove this switch statement by making "attach(int[]) a virtual method on the device base class.
	// perhaps a factory to produce the devicePtr based on the deviceType.. 
	// currently the attach logic is embeded in the constructors ..  maybe we can make that a more official
	// lifecycle for the devices..  
	// check out the make_stooge method on https://sourcemaking.com/design_patterns/factory_method/cpp/1
	// This is really how we should do this.  (methinks)
	switch (deviceType) {
  	case SENSOR_TYPE_ANALOG_PIN_ARRAY: {
	  	devicePtr = attachAnalogPinArray();
		  break;
  	}
	  case SENSOR_TYPE_DIGITAL_PIN_ARRAY: {
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
	  case DEVICE_TYPE_NEOPIXEL: {
	    devicePtr = attachNeopixel();
	  }
  	default: {
		  // TODO: publish error message
	  	publishDebug("Unknown Message Type.");
		  break;
	  }
  }

  // KW: a sort of null pointer case? TODO: maybe move this into default branch of switch above?
  if (devicePtr) {
	  devicePtr->id = nextDeviceId;
  	addDevice(devicePtr);
	  publishAttachedDevice(devicePtr->id, nameSize, 3);
  	nextDeviceId++;
  }
}

/**
 * PUBLISH_ATTACHED_DEVICE
 * MSG STRUCTURE
 * PUBLISH_ATTACHED_DEVICE | NEW_DEVICE_INDEX | NAME_STR_SIZE | NAME
 *
 */
void publishAttachedDevice(int id, int nameSize, int namePos){
  Serial.write(MAGIC_NUMBER);
  int size = 1 + /* PUBLISH_ATTACHED_DEVICE */ + 1 /* NEW_DEVICE_INDEX */ + 1 /* NAME_STR_SIZE */+ nameSize /* NAME */;
  Serial.write(size); // # of bytes to follow

  Serial.write(PUBLISH_ATTACHED_DEVICE); /* PUBLISH_ATTACHED_DEVICE */
  Serial.write(id); /* NEW_DEVICE_INDEX */
  Serial.write(nameSize); /* NAME_STR_SIZE */

  for (int i = 0; i  < nameSize; i++) { /* NAME */
    Serial.write(ioCmd[namePos+i]);
  }

  Serial.flush();
}

/**
 * detachDevice - this method will walk through the device list
 * and remove the first one that has an id that matches the
 * one passed in.
 */
void detachDevice(int id) {
  // TODO: more effecient device detach/removal by walking the list
  // with a pointer
  int numDevices = deviceList.size();
  for (int i = 0; i < numDevices; i++) {
    Device* device=deviceList.get(i);
    if (device->id == id) {
      delete device;
      deviceList.remove(i);
      break;
    }
  }
}

Device* attachAnalogPinArray() {
  Device* pinArray = new MrlAnalogPinArray();
  // TODO: add the actual pins to this analog pin array.
  //deviceList.add(pinArray); config[] something ?
  return pinArray;
}

Device* attachDigitalPinArray() {
  Device* pinArray = new MrlDigitalPinArray();
  // set the device type to analog pin array
  // TODO: add the list of pins to the array <-- not necessary until a request to read/poll a pin is received
  // KW: Ok, so a digital pin array is something you should dynamically specify which pins are in it ?
  return pinArray;
}

// KW: this should move into a device.attach() method on the MrlServo class/
// get rid of the attach* methods here and move them to the device classes.
Device* attachServo() {
  int pin = config[0];
  MrlServo* mrlServo = new MrlServo(pin);
  return mrlServo;
}

Device* attachPulse() {
  MrlPulse* device = new MrlPulse();
  return device;
}

Device* attachUltrasonic() {
  MrlUltrasonic* device = new MrlUltrasonic();
  return device;
}

Device* attachStepper() {
  MrlStepper* device = new MrlStepper();
  return device;
}

Device* attachMotor() {
  MrlMotor* device = new MrlMotor();
  return device;
}

Device* attachI2C(){
  MrlI2CDevice* device = new MrlI2CDevice();
  return device;
}

Device* attachNeopixel() {
  int pin=config[0];
  int numPixel=config[1];
  MrlNeopixel* device = new MrlNeopixel(pin,numPixel);
  return device;
}

/**
 * ATTACH DEVICES END
**********************************************************************/


/***********************************************************************
 * PUBLISH DEVICES BEGIN
 */

/**
 * send an error message/code back to MRL.
 * MAGIC_NUMBER|2|PUBLISH_MRLCOMM_ERROR|ERROR_CODE
 */
// KW: remove this, force an error message.
void publishError(int type) {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // bytes to follow, size = 1 FN + 1 TYPE
  Serial.write(PUBLISH_MRLCOMM_ERROR);
  Serial.write(type);
  Serial.flush();
}

/**
 * Send an error message along with the error code
 * 
 */
void publishError(int type, String message) {
  Serial.write(MAGIC_NUMBER);
  Serial.write(3+message.length()); // size = 1 FN + 1 TYPE
  Serial.write(PUBLISH_MRLCOMM_ERROR);
  Serial.write(type);
  // TODO: ensure that this is decoded on the java side properly.
  Serial.print(message);
  Serial.flush();
}

/**
 * Publish the MRLComm message
 * MAGIC_NUMBER|2|MRLCOMM_VERSION
 */
void publishVersion() {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // size
  Serial.write(PUBLISH_VERSION);
  Serial.write((byte)MRLCOMM_VERSION);
  Serial.flush();
}

/**
 * publishStatus
 * This method is for performance profiling, it returns back the amount of time
 * it took to run the loop() method and how much memory was free after that 
 * loop method ran.
 * 
 * MAGIC_NUMBER|7|[loadTime long0,1,2,3]|[freeMemory int0,1]
 */
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

/**
 * Publish the acknowledgement of the command received and processed.
 * MAGIC_NUMBER|2|PUBLISH_MESSAGE_ACK|FUNCTION
 */
void publishCommandAck() {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // bytes to follow, size 1 FN + 1 bytes (the function that we're acking.)
  Serial.write(PUBLISH_MESSAGE_ACK);
  // the function that we're ack-ing
  Serial.write(ioCmd[0]);
  Serial.flush();
}

/**
 * Publish Debug - return a text debug message back to the java based arduino service in MRL
 * MAGIC_NUMBER|1+MSG_LENGTH|MESSAGE_BYTES
 * 
 * This method will publish a string back to the Arduino service for debugging purproses.
 * 
 */
void publishDebug(String message) {
  if (debug) {
    // NOTE-KW:  If this method gets called excessively I have seen memory corruption in the 
    // arduino where it seems to be getting a null string passed in as "message"
    // very very very very very odd..  I suspect a bug in the arduino hardware/software
    Serial.flush();
    Serial.write(MAGIC_NUMBER);
    Serial.write(1+message.length());
    Serial.write(PUBLISH_DEBUG);
    Serial.print(message);
    Serial.flush();
  }
}

/**
 * publishBoardInfo()
 * MAGIC_NUMBER|2|PUBLISH_BOARD_INFO|BOARD
 * return the board type (mega/uno) that can use in javaland for the pin layout
 */
void publishBoardInfo() {
  Serial.write(MAGIC_NUMBER);
  Serial.write(2); // bytes which follow
  Serial.write(PUBLISH_BOARD_INFO);
  Serial.write(BOARD);
  Serial.flush();
}

/** ================= publish methods end ================== */
