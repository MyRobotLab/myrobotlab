/**
*
* @author GroG (at) myrobotlab.org
*
* This file is part of MyRobotLab.
*
* Enjoy !
*
* MRLComm.ino
* -----------------
* Purpose: support servos, sensors, analog & digital polling
* oscope, motors, range sensors, pingdar & steppers.
*
* Requirements: MyRobotLab running on a computer & a serial connection
*
*/

#include <Servo.h>
#include<MRLComm.h>


MRLComm mrl = MRLComm();

void setup() {

    Serial.begin(57600);
    mrl.setup(&Serial);
}

void loop() {

  // process mrl commands
  // servo control, sensor, control send & recieve
  // oscope, analog polling, digital polling etc..
  // mrl messages
  mrl.process();

  // example how to
  // send 3 vars to mrl
  /*
  int ax = 28;
  int ay = 583;
  int az = 32767;

  mrl.startMsg();
  mrl.append(ax);
  mrl.append(ay);
  mrl.append(az);
  mrl.sendMsg();
  */

}