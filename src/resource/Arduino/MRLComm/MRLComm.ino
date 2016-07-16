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
#include <Servo.h> // mac needs this removed? because it causes error on upload in Mac
#include "MrlComm.h"


#ifndef MrlServo_h
#define MrlServo_h

#include <Servo.h>
#include "Device.h"
#include "MrlMsg.h"

// servo event types
// ===== published sub-types based on device type begin ===
#define  SERVO_EVENT_STOPPED          1
#define  SERVO_EVENT_POSITION_UPDATE  2
// ===== published sub-types based on device type begin ===

/**
 * Servo Device
 */
class MrlServo : public Device {
  private:
    Servo* servo; // servo pointer - in case our device is a servo
    int pin;
    bool isMoving;
    bool isSweeping;
    int targetPos;
    float currentPos;
    int speed; // servos have a "speed" associated with them that's not part of the base servo driver
    bool eventsEnabled;
    float step;
    int min;
    int max;
    unsigned long lastUpdate;
  public:
    MrlServo();
    ~MrlServo();
    bool deviceAttach(unsigned char config[], int configSize);
    void attach(int pin);
    void detach();
    void update();
    void publishServoEvent(int eventType);
    void servoWrite(int position);
    void servoEventEnabled(int value);
    void servoWriteMicroseconds(int position);
    void setSpeed(int speed);
    void startSweep(int min, int max, int step);
    void stopSweep();
};

#endif


#ifndef MrlI2cBus_h
#define MrlI2cBus_h

#include "Device.h"
#include "MrlMsg.h"

#define WIRE Wire
#include <Wire.h>

/**
 * I2C bus
 * TODO:KW? don't allow this class to write directly to the global serial port
 * device classes shouldn't have a direct handle to the serial port, rather have
 * a mrlmessage class that it returns.
 * TODO: Mats
 * The I2CBus device represents one I2C bus.
 * It's the SDA (data line) and SCL pins (clock line) that is used to
 * communicate with any device that uses the i2c protocol on that bus.
 * It is NOT a representation of the addressable i2c devices, just the bus
 * On Arduino Uno that's pins A4 and A5, Mega 20 and 21, Leonardo 2 and 3,
 * The pin assignment is defined in Wire.h so it will change to the correct
 * pins at compile time. We don't have to worry here.
 * However some other i2c implementations exist's so that more pins can be used
 * for i2c communication. That is not supported here yet.
 *
 */
class MrlI2CBus : public Device {
  private:
  public:
    MrlI2CBus();
    void i2cRead(unsigned char* ioCmd);
    void i2cWrite(unsigned char* ioCmd);
    void i2cWriteRead(unsigned char* ioCmd);
    void update();
};

#endif



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

  // check to see if the "Arduino" device is attached ..
  // it should(must) be device 0 !

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

