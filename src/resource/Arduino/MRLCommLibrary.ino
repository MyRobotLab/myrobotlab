/*==================== MRLCOMM.H BEGIN ====================*/
/**
*
* @author GroG (at) myrobotlab.org
*
* This file is part of MyRobotLab.
*
* Enjoy !
*
* MRLComm Library
* -----------------
* Purpose: support servos, sensors, analog & digital polling
* oscope, motors, range sensors, pingdar & steppers.
*
* Requirements: MyRobotLab running on a computer & a serial connection
*
*/

// #ifndef MRLComm_h
// #define MRLComm_h

#include "Servo.h"

// ----------  MRLCOMM FUNCTION INTERFACE BEGIN -----------
#define MRLCOMM_VERSION				19

// serial protocol functions
#define MAGIC_NUMBER  					170 // 10101010

// MRL ---> Arduino methods
#define DIGITAL_WRITE        			0
#define DIGITAL_VALUE        			1
#define ANALOG_WRITE         			2
#define ANALOG_VALUE         			3
#define PINMODE              			4
#define PULSE_IN             			5
#define SERVO_ATTACH         			6
#define SERVO_WRITE          			7
#define SERVO_SET_MAX_PULSE  			8
#define SERVO_DETACH         			9
#define SET_PWM_FREQUENCY    			11
#define SET_SERVO_SPEED           		12
#define ANALOG_READ_POLLING_START	 	13
#define ANALOG_READ_POLLING_STOP	 	14
#define DIGITAL_READ_POLLING_START	 	15
#define DIGITAL_READ_POLLING_STOP		16
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
#define MRLCOMM_ERROR					29

#define PINGDAR_ATTACH              	30
#define PINGDAR_START             		31
#define PINGDAR_STOP              		32
#define PINGDAR_DATA 					33

#define SENSOR_ATTACH 					34
#define SENSOR_POLLING_START			35
#define SENSOR_POLLING_STOP				36
#define SENSOR_DATA 					37

#define SERVO_SWEEP_START				38
#define SERVO_SWEEP_STOP				39

// callback event - e.g. position arrived
// MSG MAGIC | SZ | SERVO-INDEX | POSITION
#define SERVO_EVENTS_ENABLE				40
#define SERVO_EVENT						41

#define LOAD_TIMING_ENABLE				42
#define LOAD_TIMING_EVENT				43

#define STEPPER_ATTACH					44
#define STEPPER_MOVE					45
#define STEPPER_STOP					46
#define STEPPER_RESET					47

#define STEPPER_EVENT					48
#define STEPPER_EVENT_STOP				1

#define STEPPER_TYPE_POLOLU  			1

#define CUSTOM_MSG						50

// servo event types
#define  SERVO_EVENT_STOPPED			1
#define  SERVO_EVENT_POSITION_UPDATE 	2

// error types
#define ERROR_SERIAL					1
#define ERROR_UNKOWN_CMD				2

// sensor types
#define SENSOR_ULTRASONIC				1


// need a method to identify type of board
// http://forum.arduino.cc/index.php?topic=100557.0

#define COMMUNICATION_RESET	   252
#define SOFT_RESET			   253
#define NOP  255

// ----------  MRLCOMM FUNCTION INTERFACE END -----------

// MAX definitions
// MAX_SERVOS defined by boardtype/library
#define PINGDARS_MAX		6
#define SENSORS_MAX			12
#define STEPPERS_MAX		6

#define ECHO_STATE_START 1
#define ECHO_STATE_TRIG_PULSE_BEGIN 2
#define ECHO_STATE_TRIG_PULSE_END 3
#define ECHO_STATE_MIN_PAUSE_PRE_LISTENING 4
#define ECHO_STATE_LISTENING 5
#define ECHO_STATE_GOOD_RANGE	6
#define ECHO_STATE_TIMEOUT	7

// FIXME FIXME FIXME
// -- FIXME - modified by board type BEGIN --
// Need Arduino to do a hardware abstraction layer
// https://code.google.com/p/arduino/issues/detail?id=59
// AHAAA !! - many defintions in - pins_arduino.h !!!
// Need a "board" identifier at least !!!

#define ANALOG_PIN_COUNT 16 // mega
#define DIGITAL_PIN_COUNT 54 // mega
// #define MAX_SERVOS 48 - is defined @ compile time !!
// -- FIXME - modified by board type END --

#define ARDUINO_TYPE_INT 16; // :) type identifier - not size - but what the hell ;)

/*
* TODO - CRC for last byte
* getCommand - retrieves a command message
* inbound and outbound messages are the same format, the following represents a basic message
* format
*
* MAGIC_NUMBER|NUM_BYTES|FUNCTION|DATA0|DATA1|....|DATA(N)
*
*/

//---- data record definitions begin -----

struct sensor_type
  {
      int type;
      int trigPin;
      int echoPin;
      bool isRunning;
      int timeoutUS;
      unsigned long ts;
      unsigned long lastValue;
      int state;
      //NewPing* ping;
  };


struct stepper_type
  {
  	  int ts;
      int type;
      int index;
      int currentPos;
      int targetPos;
      int speed;
      int dir;
      bool isRunning;
      int state;
      // int dirPin;
      int step0; // step0 is dirPin is POLOLU TYPE
      int step1;
      int step2;
      int step3;
  };

// Servos
struct servo_type
{
    Servo* servo;
    int index; // index of this servo
    int speed;
    int targetPos;
    int currentPos;
    bool isMoving;

    int step; // affects speed usually 1

    // sweep related
    int min;
    int max;
    // int delay; - related to speed
    int increment;
    bool isSweeping;

    // event related
    bool eventsEnabled;
};

//---- data record definitions end -----

// TODO - all well and good .. but you dont control Servo's data (yet)
// should have a struct for it too - contains all the data info you'd want to have
// in a servo - same with stepper

struct pingdar_type
  {
      int servoIndex; // id of servo in servos array
      //int servoPos; in servo
      int sensorIndex;
      int sweepMin;
      int sweepMax;
      int step;
      bool isRunning; // needed ? - is combo of two
  };

class MRLComm {
public:

	stepper_type steppers[STEPPERS_MAX];
	sensor_type sensors[SENSORS_MAX];

	int msgSize; // the NUM_BYTES of current message

	unsigned int debounceDelay; // in ms
	long lastDebounceTime[DIGITAL_PIN_COUNT];
	byte msgBuf[64];

	servo_type servos[MAX_SERVOS];


	unsigned long loopCount;
	unsigned long lastMicros;
	int byteCount;
	unsigned char newByte;
	unsigned char ioCmd[64];  // message buffer for all inbound messages
	int readValue;

	// FIXME - normalize with sampleRate ..
	int loadTimingModulus;

	boolean loadTimingEnabled;
	unsigned long loadTime;
	// TODO - avg load time

	unsigned int sampleRate; // 1 - 65,535 modulus of the loopcount - allowing you to sample less

	int digitalReadPin[DIGITAL_PIN_COUNT];        // array of pins to read from
	int digitalReadPollingPinCount;           // number of pins currently reading
	int lastDigitalInputValue[DIGITAL_PIN_COUNT]; // array of last input values
	bool digitalTriggerOnly;      // send data back only if its different

	int analogReadPin[ANALOG_PIN_COUNT];          // array of pins to read from
	int analogReadPollingPinCount;            // number of pins currently reading
	int lastAnalogInputValue[ANALOG_PIN_COUNT];   // array of last input values
	bool analogTriggerOnly;         // send data back only if its different

	unsigned int errorCount;

	pingdar_type pingdars[PINGDARS_MAX];

	HardwareSerial *serial;

	//===custom msg interface begin===
	byte customParams[256];
	int paramBuffIndex;
	int paramCnt;
	//===custom msg interface end===

	/* could optimize - but makes for ugly code - just to save a couple clock cycles - counting to 6 :P
	not worth it
	int pingdarsRunningCount = 0;
	int pingdarsRunning[6]; // map array of running pingdars
	*/

	// void sendMsg ( int num, ... );

	MRLComm();
	void setup(HardwareSerial *s);
	boolean getCommand();
	void softReset();
	void process();
	void sendError(int type);
	void sendServoEvent(servo_type& s, int eventType);
	unsigned long getUltrasonicRange(sensor_type& sensor);
	void setPWMFrequency (int address, int prescalar);
	void removeAndShift (int array [], int& len, int removeValue);

	//===custom msg interface begin===
	void startMsg();
	//void append(const unsigned int& data);
	void append(const int& data);
	//void append(const unsigned long& data);
	//void append(const long& data);
	void sendMsg();
	//===custom msg interface end===

	bool blinkState;
};

// #endif
/*==================== MRLCOMM.H END ========================*/
/*==================== MRLCOMM.CPP BEGIN ====================*/
// FIXME #include <MRLComm.h>

MRLComm::MRLComm(){
	msgSize = 0;
	debounceDelay = 50;
	loopCount   = 0;
	lastMicros 	= 0;
	byteCount   = 0;
	newByte     = 0;

	paramCnt = 0;
	paramBuffIndex = 0;

	loadTimingModulus = 1000;
	loadTimingEnabled = false;
	loadTime = 0;
	sampleRate = 1;
	digitalReadPollingPinCount = 0;
	digitalTriggerOnly = false;
	analogReadPollingPinCount = 0;
	analogTriggerOnly = false;
	errorCount = 0;


}

//===custom msg interface begin===

void MRLComm::startMsg() {
}


void MRLComm::append(const int& data) {
	++paramCnt;
	customParams[paramBuffIndex] = ARDUINO_TYPE_INT;
	customParams[++paramBuffIndex] = (byte)(data >> 8);
	customParams[++paramBuffIndex] = ((byte) data & 0xFF);
	++paramBuffIndex;
}

/*

void MRLComm::append(const unsigned int& data) {
}

void MRLComm::append(const unsigned long& data) {
}

void MRLComm::append(const signed long& data) {
}
*/

void MRLComm::sendMsg(){

	// unbox
	serial->write(MAGIC_NUMBER);
	serial->write(paramBuffIndex + 2); // = param buff size + FN + paramCnt
	//serial->write(2); // = param buff size + FN + paramCnt
	serial->write(CUSTOM_MSG);
	serial->write(paramCnt);

	for (int i = 0; i < paramBuffIndex; ++i){
		serial->write(customParams[i]);
	}

	paramCnt = 0;
	paramBuffIndex = 0;
}


//===custom msg interface end===


void MRLComm::setup(HardwareSerial *s) {
	serial = s;
	//Serial.begin(rate);
	softReset();
}


void MRLComm::softReset()
{
	for (int i = 0; i < MAX_SERVOS - 1; ++i)
	{
                servo_type& s = servos[i];
		s.speed = 100;
                if (s.servo != 0){
		  s.servo->detach();
                }
	}

	for (int j = 0; j < DIGITAL_PIN_COUNT - 1; ++j)
	{
		pinMode(j, OUTPUT);
	}


	digitalReadPollingPinCount = 0;
	analogReadPollingPinCount = 0;
	loopCount = 0;

}

void MRLComm::setPWMFrequency (int address, int prescalar)
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

void MRLComm::removeAndShift (int array [], int& len, int removeValue)
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

boolean MRLComm::getCommand()
{
	// handle serial data begin
	if (serial->available() > 0)
	{
		//blink LED to indicate activity
		blinkState = !blinkState;

		// read the incoming byte:
		newByte = serial->read();
		++byteCount;

		// checking first byte - beginning of message?
		if (byteCount == 1 && newByte != MAGIC_NUMBER)
		{
			++errorCount;
			sendError(ERROR_SERIAL);

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

void MRLComm::process() {

	++loopCount;

	if (getCommand())
	{
		switch (ioCmd[0])
		{

		case DIGITAL_WRITE:{
			digitalWrite(ioCmd[1], ioCmd[2]);
			break;
		}

		case ANALOG_WRITE:{
			analogWrite(ioCmd[1], ioCmd[2]);
			break;
		}

		case PINMODE:{
			pinMode(ioCmd[1], ioCmd[2]);
			break;
		}

		case SERVO_ATTACH:{
			servo_type& s = servos[ioCmd[1]];
			s.index = ioCmd[1];
			if (s.servo == NULL){
				s.servo = new Servo();
			}
			s.servo->attach(ioCmd[2]);
			s.step = 1;
			s.eventsEnabled = false;
			break;
		}

		case SERVO_SWEEP_START:{
			servo_type& s = servos[ioCmd[1]];
			s.min = ioCmd[2];
			s.max = ioCmd[3];
			s.step = ioCmd[4];
			s.isMoving = true;
			s.isSweeping = true;
			break;
		}

		case SERVO_SWEEP_STOP:{
			servo_type& s = servos[ioCmd[1]];
			s.isMoving = false;
			s.isSweeping = false;
			break;
		}

		case SERVO_WRITE:{
			servo_type& s = servos[ioCmd[1]];
			if (s.speed == 100 && s.servo != 0)// move at regular/full 100% speed
			{
				s.targetPos = ioCmd[2];
				s.currentPos = ioCmd[2];
				s.isMoving = false;
				s.servo->write(ioCmd[2]);
				if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_STOPPED);
			} else if (s.speed < 100 && s.speed > 0) {
				s.targetPos = ioCmd[2];
				s.isMoving = true;
			}
			break;
		}

		case SERVO_EVENTS_ENABLE:{
			servo_type& s = servos[ioCmd[1]];
			s.eventsEnabled = ioCmd[2];
			break;
		}

		case LOAD_TIMING_ENABLE:{
			loadTimingEnabled = ioCmd[1];
			//loadTimingModulus = ioCmd[2];
			loadTimingModulus = 1000;
			break;
		}

		case SERVO_WRITE_MICROSECONDS:{
			// TODO - incorporate into speed control etc
			// normalize - currently by itself doesn't effect events
			// nor is it involved in speed control
			servo_type& s = servos[ioCmd[1]];
			if (s.servo != 0) {
				// 1500 midpoint
				s.servo->writeMicroseconds(ioCmd[2]);
			}

			break;
		}

		case SET_SERVO_SPEED:{
			// setting the speed of a servo
			servo_type& servo = servos[ioCmd[1]];
			servo.speed = ioCmd[2];
			break;
		}

		case SERVO_DETACH:{
			servo_type& s = servos[ioCmd[1]];
			if (s.servo != 0){
			  s.servo->detach();
			}
			break;
		}

		case SET_PWM_FREQUENCY:{
			setPWMFrequency (ioCmd[1], ioCmd[2]);
			break;
		}

		case ANALOG_READ_POLLING_START:{
			analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT - if already set don't increment
			++analogReadPollingPinCount;
			break;
		}

		case ANALOG_READ_POLLING_STOP:{
			// TODO - MAKE RE-ENRANT
			removeAndShift(analogReadPin, analogReadPollingPinCount, ioCmd[1]);
			break;
		}

		case DIGITAL_READ_POLLING_START:{
			// TODO - MAKE RE-ENRANT
			digitalReadPin[digitalReadPollingPinCount] = ioCmd[1]; // put on polling read list
			++digitalReadPollingPinCount;
			break;
		}

		case DIGITAL_READ_POLLING_STOP:{
			// TODO - MAKE RE-ENRANT
			removeAndShift(digitalReadPin, digitalReadPollingPinCount, ioCmd[1]);
			break;
		}

		case SET_ANALOG_TRIGGER:{
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
			analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
			++analogReadPollingPinCount;
			break;
		}

		case DIGITAL_DEBOUNCE_ON:{
			// debounceDelay = 50;
			debounceDelay = ((ioCmd[1]<<8) + ioCmd[2]);
			break;
		}

		case DIGITAL_DEBOUNCE_OFF:{
			debounceDelay = 0;
			break;
		}

		case DIGITAL_TRIGGER_ONLY_ON:{
			digitalTriggerOnly = true;
			break;

		case DIGITAL_TRIGGER_ONLY_OFF:
			digitalTriggerOnly = false;
			break;
		}

		case SET_SERIAL_RATE:
		{
			serial->end();
			delay(500);
			serial->begin(ioCmd[1]);
			break;
		}

		case GET_MRLCOMM_VERSION:{
			serial->write(MAGIC_NUMBER);
			serial->write(2); // size
			serial->write(GET_MRLCOMM_VERSION);
			serial->write((byte)MRLCOMM_VERSION);
			break;
			}

		case PULSE_IN: {
		    // might need to hack the pulseIn lib - but would
		    // like to do it without delay
			// http://arduino.cc/en/Tutorial/BlinkWithoutDelay
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

			//sendMsg(4, ANALOG_VALUE, analogReadPin[i], readValue >> 8, readValue & 0xFF);
 			//sendMsg(5, PULSE_IN, duration >> 24, duration >> 16, duration >> 8, duration & 0xFF);
 			//sendMsg(6, SENSOR_DATA, 47, duration >> 24, duration >> 16, duration >> 8, duration & 0xFF);


			serial->write(MAGIC_NUMBER);
			serial->write(5); // size 1 FN + 4 bytes of unsigned long
			serial->write(PULSE_IN);
            // write the long value out
			serial->write((byte)(duration >> 24));
			serial->write((byte)(duration >> 16));
			serial->write((byte)(duration >> 8));
			serial->write((byte)duration & 0xFF);


			break;
		}

		case SET_SAMPLE_RATE:{
			// 2 byte int - valid range 1-65,535
			sampleRate = (ioCmd[1]<<8) + ioCmd[2];
			if (sampleRate == 0)
				{ sampleRate = 1; } // avoid /0 error - FIXME - time estimate param
			break;
		}

		case SOFT_RESET:{
			softReset();
			break;
		}

		case STEPPER_ATTACH:{
			stepper_type& stepper = steppers[ioCmd[1]];
			stepper.index = ioCmd[1];
			stepper.type = ioCmd[2];
			stepper.isRunning = false;
			stepper.currentPos = 0;
			stepper.targetPos = 0;
			stepper.dir = 0;
			stepper.speed = 100;

			if (stepper.type == STEPPER_TYPE_POLOLU) {
				stepper.step0 = ioCmd[3]; // dir pin
				stepper.step1 = ioCmd[4]; // step pin
			} else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

		case STEPPER_RESET:{
			stepper_type& stepper = steppers[ioCmd[1]];
			stepper.isRunning = false;
			stepper.currentPos = 0;
			stepper.targetPos = 0;
			stepper.dir = 0;
			stepper.speed = 100;
			break;
		}

		case STEPPER_MOVE:{
			stepper_type& stepper = steppers[ioCmd[1]];
			if (stepper.type == STEPPER_TYPE_POLOLU) {
				stepper.isRunning = true;

				stepper.targetPos = stepper.currentPos + (ioCmd[2]<<8) + ioCmd[3];
				// relative position & direction
				if (stepper.targetPos < 0) {
					// direction
					digitalWrite(stepper.step0, 1);
				} else {
					digitalWrite(stepper.step0, 0);
				}
			} else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

		case STEPPER_STOP:{
			stepper_type& stepper = steppers[ioCmd[1]];
			if (stepper.type == STEPPER_TYPE_POLOLU) {
				stepper.isRunning = false;
				stepper.targetPos = stepper.currentPos;

				serial->write(MAGIC_NUMBER);
				serial->write(5); // size = 1 FN + 1 eventType + 1 index + 1 curPos
				serial->write(STEPPER_EVENT);
				serial->write(STEPPER_EVENT_STOP);
				serial->write(stepper.index); // send my index
				serial->write(stepper.currentPos >> 8);   // MSB
				serial->write(stepper.currentPos & 0xFF);	// LSB
			} else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

			// --VENDOR CODE BEGIN--
			// --VENDOR CODE END--

		case PINGDAR_ATTACH:{
			int pingdarIndex = ioCmd[1];
			pingdar_type& pingdar = pingdars[pingdarIndex];
			pingdar.servoIndex = ioCmd[2];
			pingdar.sensorIndex = ioCmd[3];
			pingdar.step = 1;
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
				//sensor.ping = new NewPing(sensor.trigPin, sensor.echoPin, 100);
			}

			break;
		}

		case SENSOR_POLLING_START:{
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.isRunning = true;

			// I'm used to ms - and would need to change some
			// interfaces if i was to support inbound longs
			//sensor.timeoutUS = ioCmd[2] * 1000;
			sensor.timeoutUS = 20000; // 20 ms
			sensor.state = ECHO_STATE_START;

			break;
		}

		case SENSOR_POLLING_STOP:{
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.isRunning = false;
			break;
		}

		case NOP:{
			// No Operation
			break;
		}

		default:{
		    sendError(ERROR_UNKOWN_CMD);
			break;
		}
		} // end switch

		// reset buffer
		memset(ioCmd,0,sizeof(ioCmd));
		byteCount = 0;

	} // if getCommand()

	// all reads are affected by sample rate
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
				serial->write(MAGIC_NUMBER);
				serial->write(3); // size
				serial->write(DIGITAL_VALUE);
				serial->write(digitalReadPin[i]);// Pin#
				serial->write(readValue); 	// LSB

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
				//sendMsg(4, ANALOG_VALUE, analogReadPin[i], readValue >> 8, readValue & 0xFF);

				serial->write(MAGIC_NUMBER);
				serial->write(4); //size
				serial->write(ANALOG_VALUE);
				serial->write(analogReadPin[i]);
				serial->write(readValue >> 8);   // MSB
				serial->write(readValue & 0xFF);	// LSB

	        }
			// set the last input value of this pin
			lastAnalogInputValue[analogReadPin[i]] = readValue;
		}
	}

	// update moving servos - send events if required
	for (int i = 0; i < MAX_SERVOS; ++i)
	{
		servo_type& s = servos[i];
		if (s.isMoving && s.servo != 0){
			if (s.currentPos != s.targetPos)
			{
				// caclulate the appropriate modulus to drive
				// the servo to the next position
				// TODO - check for speed > 0 && speed < 100 - send ERROR back?
				int speedModulus = (100 - s.speed) * 10;
				if (loopCount % speedModulus == 0)
				{
					int increment = s.step * ((s.currentPos < s.targetPos)?1:-1);
					// move the servo an increment
					s.currentPos = s.currentPos + increment;
					s.servo->write(s.currentPos);
					if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_POSITION_UPDATE);
				}
			} else {
				if (s.isSweeping) {
					if (s.targetPos == s.min){
						s.targetPos = s.max;
					} else {
						s.targetPos = s.min;
					}
				} else {
					if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_STOPPED);
					s.isMoving = false;
				}
			}
		}
	}

	unsigned long ts;

	for (int i = 0; i < SENSORS_MAX; ++i) {
		sensor_type& sensor = sensors[i];
		if (sensor.isRunning == true){
			if (sensor.type == SENSOR_ULTRASONIC){

				// we are running & have an ultrasonic (ping) sensor
				// check to see what state we  are in

				if (sensor.state == ECHO_STATE_START){
					// trigPin prepare - start low for an
					// upcoming high pulse
					pinMode(sensor.trigPin, OUTPUT);
					digitalWrite(sensor.trigPin, LOW);

					// put the echopin into a high state
					// is this necessary ???
					pinMode(sensor.echoPin, OUTPUT);
					digitalWrite(sensor.echoPin, HIGH);

					ts = micros();
					if (ts - sensor.ts > 2){
						sensor.ts = ts;
						sensor.state = ECHO_STATE_TRIG_PULSE_BEGIN;
					}
				} else if (sensor.state == ECHO_STATE_TRIG_PULSE_BEGIN){

					// begin high pulse for at least 10 us
					pinMode(sensor.trigPin, OUTPUT);
					digitalWrite(sensor.trigPin, HIGH);

					ts = micros();
					if (ts - sensor.ts > 10){
						sensor.ts = ts;
						sensor.state = ECHO_STATE_TRIG_PULSE_END;
					}
				} else if (sensor.state == ECHO_STATE_TRIG_PULSE_END){
					// end of pulse
					pinMode(sensor.trigPin, OUTPUT);
					digitalWrite(sensor.trigPin, LOW);

					sensor.state = ECHO_STATE_MIN_PAUSE_PRE_LISTENING;
					sensor.ts = micros();
				} else if (sensor.state == ECHO_STATE_MIN_PAUSE_PRE_LISTENING){

					ts = micros();
					if (ts - sensor.ts > 1500){
						sensor.ts = ts;

						// putting echo pin into listen mode
						pinMode(sensor.echoPin, OUTPUT);
						digitalWrite(sensor.echoPin, HIGH);
						pinMode(sensor.echoPin, INPUT);

						sensor.state = ECHO_STATE_LISTENING;
					}

				} else if (sensor.state == ECHO_STATE_LISTENING) {
					// timeout or change states..
					int value = digitalRead(sensor.echoPin);
					ts = micros();

					if (value == LOW) {
						sensor.lastValue = ts - sensor.ts;
						sensor.ts = ts;
						sensor.state = ECHO_STATE_GOOD_RANGE;
					} else if (ts - sensor.ts > sensor.timeoutUS) {
						sensor.state = ECHO_STATE_TIMEOUT;
						sensor.ts = ts;
						sensor.lastValue = 0;
					}

				} else if (sensor.state == ECHO_STATE_GOOD_RANGE || sensor.state == ECHO_STATE_TIMEOUT) {
					serial->write(MAGIC_NUMBER);
					serial->write(6); // size 1 FN + 4 bytes of unsigned long
					serial->write(SENSOR_DATA);
					serial->write(i);
		            // write the long value out
					serial->write((byte)(sensor.lastValue >> 24));
					serial->write((byte)(sensor.lastValue >> 16));
					serial->write((byte)(sensor.lastValue >> 8));
					serial->write((byte) sensor.lastValue & 0xFF);
					sensor.state = ECHO_STATE_START;
				} // end else if

			} // if (sensor.type == SENSOR_ULTRASONIC)

		} // end isRunning

	} // end for each sensor

	// TODO - brake - speed - fractional stepping - other stepper types
	for (int i = 0; i < STEPPERS_MAX; ++i) {
		stepper_type& stepper = steppers[i];
		if (stepper.isRunning == true){
			if (stepper.type == STEPPER_TYPE_POLOLU){

				// direction is already set in initial STEPPER_MOVE

				if (stepper.currentPos < stepper.targetPos) {

				    // step - POLOLU has single step pin (dir is on step0)
					digitalWrite(stepper.step1, 1);
					delayMicroseconds(1); // :P should require another state? loop is ~106us min ?
					digitalWrite(stepper.step1, 0);

					stepper.currentPos++;
				} else if (stepper.currentPos > stepper.targetPos) {

				    // step - POLOLU has single step pin (dir is on step0)
					digitalWrite(stepper.step1, 1);
					delayMicroseconds(1); // :P should require another state? loop is ~106us min ?
					digitalWrite(stepper.step1, 0);

					stepper.currentPos--;

				} else {
					stepper.isRunning = false;
					stepper.currentPos = stepper.targetPos; // forcing ? :P
					serial->write(MAGIC_NUMBER);
					serial->write(5); // size = 1 FN + 1 eventType + 1 index + 1 curPos
					serial->write(STEPPER_EVENT);
					serial->write(STEPPER_EVENT_STOP);
					serial->write(stepper.index); // send my index
					serial->write(stepper.currentPos >> 8);   // MSB
					serial->write(stepper.currentPos & 0xFF);	// LSB
				}

			}
		}
	}

	unsigned long now = micros();
	loadTime = now - lastMicros; // avg outside
 	lastMicros = now;

	// report load time
	if (loadTimingEnabled && (loopCount%loadTimingModulus == 0)) {

 		// send it
		serial->write(MAGIC_NUMBER);
		serial->write(5); // size 1 FN + 4 bytes of unsigned long
		serial->write(LOAD_TIMING_EVENT);
        // write the long value out
		serial->write((byte)(loadTime >> 24));
		serial->write((byte)(loadTime >> 16));
		serial->write((byte)(loadTime >> 8));
		serial->write((byte) loadTime & 0xFF);
	}

} // end of big loop

unsigned long MRLComm::getUltrasonicRange(sensor_type& sensor){

		// added for sensors which have single pin !
		pinMode(sensor.trigPin, OUTPUT);
		digitalWrite(sensor.trigPin, LOW);
		delayMicroseconds(2);

		digitalWrite(sensor.trigPin, HIGH);
		delayMicroseconds(10);

		digitalWrite(sensor.trigPin, LOW);

		// added for sensors which have single pin !
		pinMode(sensor.echoPin, INPUT);
		// CHECKING return pulseIn(sensor.echoPin, HIGH, sensor.timeoutUS);
		// TODO - adaptive timeout ? - start big - pull in until valid value - push out if range is coming close
		return pulseIn(sensor.echoPin, HIGH);
}

void MRLComm::sendServoEvent(servo_type& s, int eventType){
  	// check type of event - STOP vs CURRENT POS

	serial->write(MAGIC_NUMBER);
	serial->write(5); // size = 1 FN + 1 INDEX + 1 eventType + 1 curPos
	serial->write(SERVO_EVENT);
	serial->write(s.index); // send my index
	// write the long value out
	serial->write(eventType);
	serial->write(s.currentPos);
	serial->write(s.targetPos);
}

void MRLComm::sendError(int type){
	serial->write(MAGIC_NUMBER);
	serial->write(2); // size = 1 FN + 1 TYPE
	serial->write(MRLCOMM_ERROR);
	serial->write(type);
}

/* SEEMED LIKE A GOOD IDEA NOT !!!!
void sendMsg ( int num, ... )
{
	va_list arguments;
	// Initializing arguments to store all values after num
	va_start ( arguments, num );

	// write header
	msgBuf[0] = MAGIC_NUMBER;
	msgBuf[1] = num;

	// copies msg payload to buffer after header
	for ( int x = 2; x < num+2; x++ )
	{
		msgBuf[x] = (byte) va_arg ( arguments, int );
	}
	va_end ( arguments );                  // Cleans up the list
	serial->write(msgBuf, num + 2);
	return;
}
*/

/*==================== MRLCOMM.CPP END ====================*/
/*==================== MRLCOMM.INO BEGIN ====================*/
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
/*==================== MRLCOMM.INO END ====================*/
