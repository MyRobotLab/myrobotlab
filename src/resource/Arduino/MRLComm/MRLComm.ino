/**
*
* @author greg (at) myrobotlab.org
*
* This file is part of MyRobotLab.
*
* MyRobotLab is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version (subject to the "Classpath" exception
* as provided in the LICENSE.txt file that accompanied this code).
*
* MyRobotLab is distributed in the hope that it will be useful or fun,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* All libraries in thirdParty bundle are subject to their own license
* requirements - please refer to http://myrobotlab.org/libraries for
* details.
*
* Enjoy !
*
* MRLComm.ino
* -----------------
* Purpose: translate serial commands into Arduino language commands,
* mostly relating to IO.  This would allow a computer to easily take
* advantage of Arduino's great IO capabilities, while cpu number crunching
* could be done on the computer (e.g. Video processing)
*
* Created 2 Februrary 2009
*
* http://myrobotlab.org
*
* TODO - create analogSensitivity (delta) & analogGain (scalar)
* References :
*      http://www.arduino.cc/en/Reference/Constants
*
*/

#include <Servo.h>

#define MRLCOMM_VERSION      10

// MAX definitions
// MAX_SERVOS defined by boardtype/library
#define PINGDARS_MAX		6
#define SENSORS_MAX			12

#define DIGITAL_WRITE        0
#define DIGITAL_VALUE        1
#define ANALOG_WRITE         2
#define ANALOG_VALUE         3
#define PINMODE              4
#define PULSE_IN             5
#define SERVO_ATTACH         6
#define SERVO_WRITE          7
#define SERVO_SET_MAX_PULSE  8
#define SERVO_DETACH         9
#define SERVO_STOP_AND_REPORT 			10
#define SET_PWM_FREQUENCY    			11
#define SET_SERVO_SPEED           		12
#define ANALOG_READ_POLLING_START	 	13
#define ANALOG_READ_POLLING_STOP	 	14
#define DIGITAL_READ_POLLING_START	 	15
#define DIGITAL_READ_POLLING_STOP	 	16
#define SET_ANALOG_TRIGGER				17
#define REMOVE_ANALOG_TRIGGER			18
#define SET_DIGITAL_TRIGGER				19
#define REMOVE_DIGITAL_TRIGGER			20
#define DIGITAL_DEBOUNCE_ON				21
#define DIGITAL_DEBOUNCE_OFF			22
#define DIGITAL_TRIGGER_ONLY_ON			23
#define DIGITAL_TRIGGER_ONLY_OFF		24
#define SET_SERIAL_RATE					25
#define GET_MRLCOMM_VERSION				26
#define SET_SAMPLE_RATE					27
#define SERVO_WRITE_MICROSECONDS		28
// TODO - make single ERROR type - with error code to be sent back
// function error(errorCode)
// error unknown command
// error bad magic number
// error serial rx error
// error ???
#define MRLCOMM_RX_ERROR				29

#define PINGDAR_ATTACH              	30
#define PINGDAR_START             		31
#define PINGDAR_STOP              		32
#define PINGDAR_DATA 					33

#define SENSOR_ATTACH 					34
#define SENSOR_POLLING_START			35
#define SENSOR_POLLING_STOP				36
#define SENSOR_DATA 					37

#define SENSOR_ULTRASONIC				1

/*
// FIXME - finish implementation Stepper* steppers[MAX_STEPPERS];
// http://arduino.cc/en/Reference/StepperStep
#define STEPPER_ATTACH				 	xx
#define STEPPER_DETACH				 	xx
#define STEPPER_STEP				 	xx
// lame - shuold determine max stepper based on board pins / pins required
#define MAX_STEPPERS 2 
*/

// need a method to identify type of board
// http://forum.arduino.cc/index.php?topic=100557.0

#define COMMUNICATION_RESET	   252
#define SOFT_RESET			   253
#define SERIAL_ERROR           254
#define NOP  255

#define MAGIC_NUMBER    170 // 10101010

// pin services
#define POLLING_MASK 1
#define TRIGGER_MASK 2
// TODO #define SERVO_SWEEP

// --VENDOR DEFINE SECTION BEGIN--
// --VENDOR DEFINE SECTION END--

// -- FIXME - modified by board type BEGIN --
#define ANALOG_PIN_COUNT 16 // mega
#define DIGITAL_PIN_COUNT 54 // mega
// #define MAX_SERVOS 48 - is defined @ compile time !! 
// -- FIXME - modified by board type END --

/*
* getCommand - retrieves a command message
* inbound and outbound messages are the same format, the following represents a basic message
* format
*
* MAGIC_NUMBER|NUM_BYTES|FUNCTION|DATA0|DATA1|....|DATA(N)
*
*/

int msgSize = 0; // the NUM_BYTES of current message

unsigned int debounceDelay = 50; // in ms
long lastDebounceTime[DIGITAL_PIN_COUNT];

Servo servos[MAX_SERVOS];
int servoSpeed[MAX_SERVOS];    // 0 - 100 corresponding to the 0.0 - 1.0 Servo.setSpeed - not a float at this point
int servoTargetPosition[MAX_SERVOS];  // when using a fractional speed - servo's must remember their end destination
int servoCurrentPosition[MAX_SERVOS]; // when using a fractional speed - servo's must remember their end destination
int movingServos[MAX_SERVOS];		  // array of servos currently moving at some fractional speed
int movingServosCount = 0;            // number of servo's currently moving at fractional speed

unsigned long loopCount     = 0;
int byteCount               = 0;
unsigned char newByte 		= 0;
unsigned char ioCmd[64];  // message buffer for all inbound messages
int readValue;
unsigned int sampleRate = 1; // 1 - 65,535 modulus of the loopcount - allowing you to sample less


int digitalReadPin[DIGITAL_PIN_COUNT];        // array of pins to read from
int digitalReadPollingPinCount = 0;           // number of pins currently reading
int lastDigitalInputValue[DIGITAL_PIN_COUNT]; // array of last input values
bool digitalTriggerOnly	= false;      // send data back only if its different

int analogReadPin[ANALOG_PIN_COUNT];          // array of pins to read from
int analogReadPollingPinCount = 0;            // number of pins currently reading
int lastAnalogInputValue[ANALOG_PIN_COUNT];   // array of last input values
bool analogTriggerOnly = false;         // send data back only if its different

unsigned int errorCount = 0;

// TODO - all well and good .. but you dont control Servo's data (yet)
// should have a struct for it too - contains all the data info you'd want to have
// in a servo - same with stepper

typedef struct
  {
      int servoIndex; // id of servo in servos array
      int servoPos;
      int sensorIndex;
      int sweepMin;
      int sweepMax;
      int step;
      bool isRunning;
  }  pingdar_type;

pingdar_type pingdars[PINGDARS_MAX];

/* could optimize - but makes for ugly code - just to save a couple clock cycles - counting to 6 :P
not worth it
int pingdarsRunningCount = 0;
int pingdarsRunning[6]; // map array of running pingdars
*/

typedef struct
  {
 	  int type;
 	  int trigPin;
      int echoPin;
      bool isRunning;
  }  sensor_type;

sensor_type sensors[SENSORS_MAX];

void setup() {
	Serial.begin(57600);        // connect to the serial port

	softReset();

	// --VENDOR SETUP BEGIN--
	// --VENDOR SETUP END--
}

void softReset()
{
	for (int i = 0; i < MAX_SERVOS - 1; ++i)
	{
		servoSpeed[i] = 100;
		servos[i].detach();
	}

	for (int j = 0; j < DIGITAL_PIN_COUNT - 1; ++j)
	{
		pinMode(j, OUTPUT);
	}


	digitalReadPollingPinCount = 0;
	analogReadPollingPinCount = 0;
	loopCount = 0;

}

void setPWMFrequency (int address, int prescalar)
{
	int clearBits = 0x07;
	if (address == 0x25)
	{
		TCCR0B &= ~clearBits;
		TCCR0B |= prescalar;
	} else if (address == 0x2E)
	{
		TCCR1B &= ~clearBits;
		TCCR1B |= prescalar;
	} else if (address == 0xA1)
	{
		TCCR2B &= ~clearBits;
		TCCR2B |= prescalar;
	}

}

void removeAndShift (int array [], int& len, int removeValue)
{
	int pos = -1;

	if (len == 0)
	{
		return;
	}

	// find position of value
	for (int i = 0; i < len; ++i)
	{
		if (removeValue == array[i])
		{
			pos = i;
			break;
		}
	}
	// if at the end just decrement size
	if (pos == len - 1)
	{
		--len;
		return;
	}

	// if found somewhere else shift left
	if (pos < len && pos > -1)
	{
		for (int j = pos; j < len - 1; ++j)
		{
			array[j] = array[j+1];
		}
		--len;
	}
}

boolean getCommand ()
{
	// handle serial data begin
	if (Serial.available() > 0)
	{
		// read the incoming byte:
		newByte = Serial.read();
		++byteCount;

		// checking first byte - beginning of message?
		if (byteCount == 1 && newByte != MAGIC_NUMBER)
		{
			// ERROR !!!!!
			// TODO - call modulus error method - notify sender
			++errorCount;
			
			Serial.write(MAGIC_NUMBER);
			Serial.write(1); // size
			Serial.write(MRLCOMM_RX_ERROR);
			
			// reset - try again
			byteCount = 0; 
			return false;
		}
		
		if (byteCount == 2)
		{
		   // get the size of message
		   // todo check msg < 64 (MAX_MSG_SIZE)
		   msgSize = newByte;
		}
		
		if (byteCount > 2) {
		  // fill in msg data - (2) headbytes -1 (offset)
		  ioCmd[byteCount - 3] = newByte;
		}
		
		// if received header + msg
		if (byteCount == 2 + msgSize)
		{
          return true;
		}
	} // if Serial.available

	return false;
}

void moveServo(bool isWriteMicroSecond){

	if (servoSpeed[ioCmd[1]] == 100) // move at regular/full 100% speed
	{
		// move at regular/full 100% speed
		// although not completely accurate
		// target position & current position are
		// updated immediately
		if (isWriteMicroSecond) {
			int ms = (ioCmd[2]<<8) + ioCmd[3];
			servos[ioCmd[1]].writeMicroseconds(ms);
		} else {
			servos[ioCmd[1]].write(ioCmd[2]);
		}
		servoTargetPosition[ioCmd[1]] = ioCmd[2];
		servoCurrentPosition[ioCmd[1]] = ioCmd[2];
	} else if (servoSpeed[ioCmd[1]] < 100 && servoSpeed[ioCmd[1]] > 0) {
		// start moving a servo at fractional speed
		servoTargetPosition[ioCmd[1]] = ioCmd[2];
		movingServos[movingServosCount]=ioCmd[1];
		++movingServosCount;
	} else {
		// NOP - 0 speed - don't move
	}
}

void loop () {

	++loopCount;

	if (getCommand())
	{
		switch (ioCmd[0])
		{
		case DIGITAL_WRITE:
			digitalWrite(ioCmd[1], ioCmd[2]);
			break;
		case ANALOG_WRITE:
			analogWrite(ioCmd[1], ioCmd[2]);
			break;
		case PINMODE:
			pinMode(ioCmd[1], ioCmd[2]);
			break;
		case SERVO_ATTACH:
			servos[ioCmd[1]].attach(ioCmd[2]);
			break;
		case SERVO_WRITE:
			moveServo(false);
			break;
		case SERVO_WRITE_MICROSECONDS:
		    moveServo(true);
			break;
		case SERVO_STOP_AND_REPORT:
			// a stop can only be issued to a moving servo under speed control
			if (servoSpeed[ioCmd[1]] < 100 && servoSpeed[ioCmd[1]] > 0) {
				servoTargetPosition[ioCmd[1]] = servoCurrentPosition[ioCmd[1]];
				removeAndShift(movingServos, movingServosCount, ioCmd[1]);
			} 
			break;
		case SET_SERVO_SPEED:
			// setting the speed of a servo
			servoSpeed[ioCmd[1]]=ioCmd[2];
			break;
		case SERVO_SET_MAX_PULSE:
			//servos[ioCmd[1]].setMaximumPulse(ioCmd[2]);    TODO - lame fix hardware
			break;
		case SERVO_DETACH:
			servos[ioCmd[1]].detach();
			break;
		case SET_PWM_FREQUENCY:
			setPWMFrequency (ioCmd[1], ioCmd[2]);
			break;
		case ANALOG_READ_POLLING_START:
			analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT - if already set don't increment
			++analogReadPollingPinCount;
			break;
		case ANALOG_READ_POLLING_STOP:
			// TODO - MAKE RE-ENRANT
			removeAndShift(analogReadPin, analogReadPollingPinCount, ioCmd[1]);
			break;
		case DIGITAL_READ_POLLING_START:
			// TODO - MAKE RE-ENRANT
			digitalReadPin[digitalReadPollingPinCount] = ioCmd[1]; // put on polling read list
			++digitalReadPollingPinCount;
			break;
		case DIGITAL_READ_POLLING_STOP:
			// TODO - MAKE RE-ENRANT
			removeAndShift(digitalReadPin, digitalReadPollingPinCount, ioCmd[1]);
			break;
		case SET_ANALOG_TRIGGER:
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
			analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
			++analogReadPollingPinCount;
			break;
		case DIGITAL_DEBOUNCE_ON:
			// debounceDelay = 50;
			debounceDelay = ((ioCmd[1]<<8) + ioCmd[2]);
			break;
		case DIGITAL_DEBOUNCE_OFF:
			debounceDelay = 0;
			break;
		case DIGITAL_TRIGGER_ONLY_ON:
			digitalTriggerOnly = true;
			break;
		case DIGITAL_TRIGGER_ONLY_OFF:
			digitalTriggerOnly = false;
			break;
		case SET_SERIAL_RATE:
			Serial.end();
			delay(500);
			Serial.begin(ioCmd[1]);
			break;
		case GET_MRLCOMM_VERSION:
			Serial.write(MAGIC_NUMBER);
			Serial.write(2); // size
			Serial.write(GET_MRLCOMM_VERSION);
			Serial.write((byte)MRLCOMM_VERSION);
			break;
		
		case PULSE_IN: {
			int trigPin = ioCmd[1];
			int echoPin = ioCmd[2];
			// TODO - implement HI/LOW value & timeout & variable delay for trigger
			
			pinMode(trigPin, OUTPUT);
			pinMode(echoPin, INPUT);
			
			digitalWrite(trigPin, LOW); 
			delayMicroseconds(2); 
			
			digitalWrite(trigPin, HIGH);
			delayMicroseconds(10); 
			 
			digitalWrite(trigPin, LOW);
			unsigned long duration = pulseIn(echoPin, HIGH);
			
			//Calculate the distance (in cm) based on the speed of sound.
			// distance = duration/58.2;
 
            Serial.write(MAGIC_NUMBER);
			Serial.write(5); // size 1 FN + 4 bytes of unsigned long
			Serial.write(PULSE_IN);
            // write the long value out
			Serial.write((byte)(duration >> 24));
			Serial.write((byte)(duration >> 16));
			Serial.write((byte)(duration >> 8));
			Serial.write((byte)duration & 0xFF);
                    
			break;
		}
		case SET_SAMPLE_RATE:
			// 2 byte int - valid range 1-65,535
			sampleRate = (ioCmd[1]<<8) + ioCmd[2];
			if (sampleRate == 0)
				{ sampleRate = 1; } // avoid /0 error - FIXME - time estimate param
			break;
		case SOFT_RESET:
			softReset();
			break;
/* FIXME - finish Arduino's version of implementation		
		case STEPPER_ATTACH:
			steppers[ioCmd[1]] = &(Stepper(ioCmd[2], ioCmd[3], ioCmd[4], ioCmd[5], ioCmd[6]));
			break;		
*/				

			// --VENDOR CODE BEGIN--
			// --VENDOR CODE END--

		case PINGDAR_ATTACH:{
			int pingdarIndex = ioCmd[1];
			pingdar_type pingdar = pingdars[pingdarIndex];
			pingdar.servoIndex = ioCmd[2];
			pingdar.sensorIndex = ioCmd[3];
			break;
		}
		
		case SENSOR_ATTACH:{
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.type = ioCmd[2];
			
			// initialize based on sensor type
			if (sensor.type == SENSOR_ULTRASONIC){
				sensor.trigPin = ioCmd[3];
				sensor.echoPin = ioCmd[4];
				pinMode(sensor.trigPin, OUTPUT);
				pinMode(sensor.echoPin, INPUT);				
			}	
			
			break;
		}
		
		case SENSOR_POLLING_START:{
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensors[sensorIndex].isRunning = true;
			break;
		}
		
		case SENSOR_POLLING_STOP:{
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.isRunning = false;
			break;
		}
	
		case NOP:
			// No Operation
			break;
			
		default:
		    // TODO - send back an error msg
		    // error (unknown command)
			break;
		}

		// reset buffer
		memset(ioCmd,0,sizeof(ioCmd));
		byteCount = 0;

	} // if getCommand()
	
	
	if (loopCount%sampleRate == 0) {
		// digital polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
		for (int i  = 0; i < digitalReadPollingPinCount; ++i)
		{
			if (debounceDelay)
			{
			  if (millis() - lastDebounceTime[digitalReadPin[i]] < debounceDelay)
			  {
			    continue;
			  }
			}
	
			// read the pin
			readValue = digitalRead(digitalReadPin[i]);
	
			// if my value is different from last time  && config - send it
			if (lastDigitalInputValue[digitalReadPin[i]] != readValue  || !digitalTriggerOnly)
			{
				Serial.write(MAGIC_NUMBER);
				Serial.write(3); // size
				Serial.write(DIGITAL_VALUE);
				Serial.write(digitalReadPin[i]);// Pin#
				Serial.write(readValue); 	// LSB
	
			    lastDebounceTime[digitalReadPin[i]] = millis();
			}
	
			// set the last input value of this pin
			lastDigitalInputValue[digitalReadPin[i]] = readValue;
		}
	
	
		// analog polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
		for (int i  = 0; i < analogReadPollingPinCount; ++i)
		{
			// read the pin
			readValue = analogRead(analogReadPin[i]);
	
			// if my value is different from last time - send it
			if (lastAnalogInputValue[analogReadPin[i]] != readValue   || !analogTriggerOnly) //TODO - SEND_DELTA_MIN_DIFF
			{
				Serial.write(MAGIC_NUMBER);
				Serial.write(4); //size
				Serial.write(ANALOG_VALUE);
				Serial.write(analogReadPin[i]);
				Serial.write(readValue >> 8);   // MSB
				Serial.write(readValue & 0xFF);	// LSB		
	         }
			// set the last input value of this pin
			lastAnalogInputValue[analogReadPin[i]] = readValue;
		}
	}
	// handle the servos going at fractional speed
	for (int i = 0; i < movingServosCount; ++i)
	{
		int servoIndex = movingServos[i];
		int speed = servoSpeed[servoIndex];
		if (servoCurrentPosition[servoIndex] != servoTargetPosition[servoIndex])
		{
			// caclulate the appropriate modulus to drive
			// the servo to the next position
			// TODO - check for speed > 0 && speed < 100 - send ERROR back?
			int speedModulus = (100 - speed) * 10;
			if (loopCount % speedModulus == 0)
			{
				int increment = (servoCurrentPosition[servoIndex]<servoTargetPosition[servoIndex])?1:-1;
				// move the servo an increment
				servos[servoIndex].write(servoCurrentPosition[servoIndex] + increment);
				servoCurrentPosition[servoIndex] = servoCurrentPosition[servoIndex] + increment;
			}
		} else {
			removeAndShift(movingServos, movingServosCount, servoIndex);
		}
	}
	
	for (int i = 0; i < PINGDARS_MAX; ++i) {
		pingdar_type p = pingdars[i];
		if (p.isRunning == true){
			// step
			// pulsein - if sensor is "running" set to false
			// no need to send 2 sets of data - let controller (arduino) break a pingdar set apart and distribute
			// get data
			// make msg
			// send back
		}
	}

	for (int i = 0; i < SENSORS_MAX; ++i) {
		sensor_type& sensor = sensors[i];
		if (sensor.isRunning == true){
			if (sensor.type == SENSOR_ULTRASONIC){
			
				digitalWrite(sensor.trigPin, LOW); 
				delayMicroseconds(2); 
			
				digitalWrite(sensor.trigPin, HIGH);
				delayMicroseconds(10); 
			 
				digitalWrite(sensor.trigPin, LOW);
				unsigned long duration = pulseIn(sensor.echoPin, HIGH);
				
				Serial.write(MAGIC_NUMBER);
				Serial.write(6); // size = 1 FN + 1 INDEX + 4 bytes of unsigned long
				Serial.write(SENSOR_DATA);
				Serial.write(i); // send my index
				// write the long value out
				Serial.write((byte)(duration >> 24));
				Serial.write((byte)(duration >> 16));
				Serial.write((byte)(duration >> 8));
				Serial.write((byte)duration & 0xFF);
			}
		}
	}


} // loop
