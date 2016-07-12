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
#include "LinkedList.h"
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
/***********************************************************************
 * GLOBAL VARIABLES
 * TODO - work on reducing globals and pass as parameters
*/
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

// ================= publish methods end ==================
