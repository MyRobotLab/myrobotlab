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
  // send back load time and memory
  mrlComm.publishBoardStatus();
} // end of big loop

