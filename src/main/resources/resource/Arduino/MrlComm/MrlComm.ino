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
   * Requirements: MyRobotLab running on a computer &amp; a serial connection
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
#include <Wire.h>
#if defined(ESP8266)
  #include <WebSocketsServer.h>
#endif

/***********************************************************************
 * GLOBAL VARIABLES
 * TODO - work on reducing globals and pass as parameters
 */
MrlComm mrlComm;
#if defined(ESP8266)
  WebSocketsServer webSocket = WebSocketsServer(81);

  void webSocketEvent(unsigned char num, WStype_t type, unsigned char* payload, unsigned int lenght){
    mrlComm.webSocketEvent(num, type, payload, lenght);
  }
#endif
/***********************************************************************
 * STANDARD ARDUINO BEGIN
 * setup() is called when the serial port is opened unless you hack the
 * serial port on your arduino
 *
 * Here we default out serial port to 115.2kbps.
 */
void setup() {

  Wire.begin();
  
	Serial.begin(115200);

	// start with standard serial & rate
#if defined(ESP8266)
  webSocket.onEvent(webSocketEvent);
  mrlComm.begin(webSocket);
#else
	mrlComm.begin(Serial);
#endif
}

/**
 * STANDARD ARDUINO LOOP BEGIN
 * This method will be called over and over again by the arduino, it is the
 * main loop any arduino sketch runs
 */
void loop() {
	// get a command and process it from
	// the serial port (if available.)
//  wdt_disable();
	if (mrlComm.readMsg()) {
		mrlComm.processCommand();
	}
	// update devices
	mrlComm.updateDevices();
	// send back load time and memory
        // driven by getBoardInfo now !!!
        // mrlComm.publishBoardStatus();
  #if defined(ESP8266)
    webSocket.loop();
  #endif
} // end of big loop


