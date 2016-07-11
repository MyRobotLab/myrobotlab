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

// TODO: this isn't ready for an official bump to mrl comm 35
// when it's ready we can update ArduinoMsgCodec  (also need to see why it's not publishing "goodtimes" anymore.)
#define MRLCOMM_VERSION         37

#include "Arduino.h"
// Included as a 3rd party arduino library from here: https://github.com/ivanseidel/LinkedList/
#include "LinkedList.h"
#include "MrlMsg.h"
#include "Pin.h"
#include "MrlServo.h"
#include "MrlI2cBus.h"
#include "MrlNeopixel.h"
#include "MrlComm.h"




/**
* FIXME - first rule of generate club is: whole file should be generated
* so this needs to be turned itno a .h if necessary - but the manual munge
* should be replaced
* 
* Addendum up for vote:
*   Second rule of generate club is , to complete the mission, this file must/should go away...  
*   It should be generated, completely.  device subclasses, #defines and all..  muahahahhah! project mayhem...
* 
*   Third rule of generate club is, if something has no code and isn't used, remove it. If it has code, move the code.
* 
*/
// TODO: the max message size isn't auto generated but
// it is in ArduinioMsgCodec










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
      // GroG: 3 Motor Types so far - probably a single MrlMotor could handle it
      // they are MotorDualPwm MotorSimpleH and MotorPulse
      // Stepper should be its own MrlStepper
    }
    void update(unsigned long loopCount) {
      // we should update the pwm values for the control of the motor device  here
      // this is potentially where the hardware specific logic could go for various motor controllers
      // L298N vs IBT2 vs other...  maybe consider a subclass for the type of motor-controller.
      // GroG : All motor controller I know of which can be driven by the Arduino fall in the
      // MotorDualPwm MotorSimpleH and MotorPulse - categories
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
    void update(unsigned long lastMicros) {
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
    void update(unsigned long lastMicros) {
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
    void update(unsigned long lastMicros) {
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
 * TODO: add a description about this device! How does it work? What is it?
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
    void update(unsigned long lastMicros) {
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
 * TODO: add a description about this device, what is it? what does it do?
 * How does it work?
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
    void update(unsigned long lastMicros) {
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





/***********************************************************************
 * GLOBAL VARIABLES
 * TODO - work on reducing globals and pass as parameters
*/
unsigned int debounceDelay = 50; // in ms
// TODO: move this onto the particular device, not global
// sensor sample rate
unsigned int sampleRate = 1; // 1 - 65,535 modulus of the loopcount - allowing you to sample less
MrlComm mrlComm;
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
  //mrlComm.softReset(); 
  // publish version on startup so it's immediately available for mrl.
  // TODO: see if we can purge the current serial port buffers
  mrlComm.publishVersion();
  // publish the board type (uno/mega)
  mrlComm.publishBoardInfo();
}

/**
 * STANDARD ARDUINO LOOP BEGIN 
 * This method will be called over and over again by the arduino, it is the 
 * main loop any arduino sketch runs
 */
void loop() {
  // increment how many times we've run
  // TODO: handle overflow here after 32k runs, i suspect this might blow up? 
  mrlComm.loopCount++;
  // get a command and process it from the serial port (if available.)
  mrlComm.readCommand();
  // update devices
  mrlComm.updateDevices();
  // update memory & timing
  mrlComm.updateStatus();
} // end of big loop

/**
 * STANDARD ARDUINO LOOP END
 */

/***********************************************************************
 * UTILITY METHODS BEGIN
 */




/**
 * UTILITY METHODS END
 ***********************************************************************/

/***********************************************************************
 * SERIAL METHODS BEGIN
 */
 

/**
 * SERIAL METHODS END
 **********************************************************************/


void sensorPollingStart() {
  // TODO: implement me.
}

void sensorPollingStop() {
  // TODO: implement me.
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
  // TODO: remove this method and use deviceDetach() / removeDevice?()
  //Pin& pin = pins[ioCmd[1]];
  //pin.isActive = false;
}

// DIGITAL_READ_POLLING_START
void digitalReadPollingStart() {
  // TODO: remove this and replace iwth deviceAttach()
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
  //debounceDelay = ((ioCmd[1] << 8) + ioCmd[2]);
}

// SET_DIGITAL_TRIGGER_ONLY
void setDigitalTriggerOnly() {
  // NOT IMPLEMENTED
  //digitalTriggerOnly = ioCmd[1];
}


// SET_SAMPLE_RATE
void setSampleRate() {
  // TODO: move this method to the device classes that have a sample rate.
  // 2 byte int - valid range 1-65,535
  //sampleRate = (ioCmd[1] << 8) + ioCmd[2];
  if (sampleRate == 0) {
    sampleRate = 1;
  } // avoid /0 error - FIXME - time estimate param
}

// SERVO_EVENTS_ENABLED
void servoEventsEnabled() {
  // Not implemented.
  // TODO: move this to the servo device class.
  // or just remove it all together.
  // publish_servo_event seem to do the same as this stub imply
}

/**
 * CONTROL METHODS END
 **********************************************************************/








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

/**
 * ATTACH DEVICES END
**********************************************************************/

/***********************************************************************
 * PUBLISH DEVICES BEGIN
 * 
 * All serial IO should happen here to publish a MRLComm message.
 * TODO: move all serial IO into a controlled place this this below...
 * TODO: create MRLCommMessage class that can just send itself!
 * 
 */








// ================= publish methods end ==================
