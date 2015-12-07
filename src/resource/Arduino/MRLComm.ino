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
// version to match with MRL
#define MRLCOMM_VERSION				25

// serial protocol functions
#define MAGIC_NUMBER  					170 // 10101010


// ----- MRLCOMM FUNCTION GENERATED INTERFACE BEGIN -----------
///// INO GENERATED DEFINITION BEGIN //////
// {publishMRLCommError Integer} 
#define PUBLISH_MRLCOMM_ERROR		1

// {getVersion} 
#define GET_VERSION		2

// {publishVersion Integer} 
#define PUBLISH_VERSION		3

// {analogReadPollingStart Integer} 
#define ANALOG_READ_POLLING_START		4

// {analogReadPollingStop Integer} 
#define ANALOG_READ_POLLING_STOP		5

// {analogWrite Integer Integer} 
#define ANALOG_WRITE		6

// {digitalReadPollingStart Integer} 
#define DIGITAL_READ_POLLING_START		7

// {digitalReadPollingStop Integer} 
#define DIGITAL_READ_POLLING_STOP		8

// {digitalWrite Integer Integer} 
#define DIGITAL_WRITE		9

// {motorAttach Motor} 
#define MOTOR_ATTACH		10

// {motorDetach Motor} 
#define MOTOR_DETACH		11

// {motorMove Motor} 
#define MOTOR_MOVE		12

// {motorMoveTo Motor} 
#define MOTOR_MOVE_TO		13

// {pinMode Integer Integer} 
#define PIN_MODE		14

// {publishCustomMsg Object[]} 
#define PUBLISH_CUSTOM_MSG		15

// {publishLoadTimingEvent Long} 
#define PUBLISH_LOAD_TIMING_EVENT		16

// {publishPin Pin} 
#define PUBLISH_PIN		17

// {publishPulse Long} 
#define PUBLISH_PULSE		18

// {publishPulseStop Integer} 
#define PUBLISH_PULSE_STOP		19

// {publishSensorData Object} 
#define PUBLISH_SENSOR_DATA		20

// {publishServoEvent Integer} 
#define PUBLISH_SERVO_EVENT		21

// {publishStepperEvent Integer} 
#define PUBLISH_STEPPER_EVENT		22

// {publishTrigger Pin} 
#define PUBLISH_TRIGGER		23

// {pulse int int int int} 
#define PULSE		24

// {pulseStop} 
#define PULSE_STOP		25

// {sensorAttach UltrasonicSensor} 
#define SENSOR_ATTACH		26

// {sensorPollingStart String int} 
#define SENSOR_POLLING_START		27

// {sensorPollingStop String} 
#define SENSOR_POLLING_STOP		28

// {servoAttach Servo Integer} 
#define SERVO_ATTACH		29

// {servoDetach Servo} 
#define SERVO_DETACH		30

// {servoSweepStart Servo} 
#define SERVO_SWEEP_START		31

// {servoSweepStop Servo} 
#define SERVO_SWEEP_STOP		32

// {servoWrite Servo} 
#define SERVO_WRITE		33

// {servoWriteMicroseconds Servo} 
#define SERVO_WRITE_MICROSECONDS		34

// {setDebounce int} 
#define SET_DEBOUNCE		35

// {setDigitalTriggerOnly Boolean} 
#define SET_DIGITAL_TRIGGER_ONLY		36

// {setLoadTimingEnabled boolean} 
#define SET_LOAD_TIMING_ENABLED		37

// {setPWMFrequency Integer Integer} 
#define SET_PWMFREQUENCY		38

// {setSampleRate int} 
#define SET_SAMPLE_RATE		39

// {setSerialRate int} 
#define SET_SERIAL_RATE		40

// {setServoEventsEnabled Servo} 
#define SET_SERVO_EVENTS_ENABLED		41

// {setServoSpeed Servo} 
#define SET_SERVO_SPEED		42

// {setStepperSpeed Integer} 
#define SET_STEPPER_SPEED		43

// {setTrigger int int int} 
#define SET_TRIGGER		44

// {softReset} 
#define SOFT_RESET		45

// {stepperAttach String} 
#define STEPPER_ATTACH		46

// {stepperDetach String} 
#define STEPPER_DETACH		47

// {stepperMoveTo String int int} 
#define STEPPER_MOVE_TO		48

// {stepperReset String} 
#define STEPPER_RESET		49

// {stepperStop String} 
#define STEPPER_STOP		50

// {stopService} 
#define STOP_SERVICE		51



// ------ non generated types begin ------

// ------ stepper types ------
#define STEPPER_TYPE_SIMPLE  			1

// ------ stepper event types ------
#define STEPPER_EVENT_STOP				1
#define STEPPER_EVENT_STEP				2

// ------ stepper event types ------
#define  SERVO_EVENT_STOPPED			1
#define  SERVO_EVENT_POSITION_UPDATE 	2

// ------ error types ------
#define ERROR_SERIAL					1
#define ERROR_UNKOWN_CMD				2
#define ERROR_ALREADY_EXISTS			3
#define ERROR_DOES_NOT_EXIST			4
#define ERROR_UNKOWN_SENSOR			5


// ------ sensor types ------
#define SENSOR_PIN						0
#define SENSOR_ULTRASONIC				1
#define SENSOR_PULSE					2

#define CUSTOM_MSG						50

// need a method to identify type of board
// http://forum.arduino.cc/index.php?topic=100557.0

#define COMMUNICATION_RESET	   252
#define SOFT_RESET			   253
#define NOP  				   255

// ----------  MRLCOMM FUNCTION INTERFACE END -----------

// MAX definitions
// MAX_SERVOS defined by boardtype/library
#define PINGDARS_MAX		6
#define SENSORS_MAX			12
#define STEPPERS_MAX		6

#define MAX_MOTORS 10

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
*              NUM_BYTES - is the number of bytes after NUM_BYTES to the end
*
*/

int msgSize = 0; // the NUM_BYTES of current message

unsigned int debounceDelay = 50; // in ms
long lastDebounceTime[DIGITAL_PIN_COUNT];
byte msgBuf[64];

// FIXME - make union of sensors
// all start with general / common section - with "type" :)
typedef struct
{
	// general
	int type; // SENSOR_PIN | SENSOR_PULSE | SENSOR_ULTRASONIC
	int state; // state - single at the moment to handle all the finite states of the sensor
	int index; // the all important index of the sensor - equivalent to the "name" - used in callbacks
	bool isActive;
	int feedbackRate;
	int debounce; 
	int rate;
	unsigned long count;
	unsigned long target;

	// srf05
	int trigPin;
	int echoPin;
	int timeoutUS;
	unsigned long ts;
	unsigned long lastValue;

	// pulse 
	int pulsePin;

}  sensor_type;

sensor_type sensors[SENSORS_MAX];

// FIXME REMOTE - STEPPER IS JUST A MOTOR TYPE
// COMAND STRUCTURE
typedef struct
{
	int ts;
	int type;
	int index; // is this redundant?
	int currentPos;
	int targetPos;
	int speed;

	int dirPin;
	int stepPin;

	// support up to 5 wire steppers
	int pin0;
	int pin1;
	int pin2;
	int pin3;
	int pin4;

}  stepper_type;

stepper_type steppers[STEPPERS_MAX];

// Servos
typedef struct
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
} servo_type;


servo_type servos[MAX_SERVOS];


// Motors - currently not used
typedef struct
{
	int index; // index of this motor
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
} motor_type;

// motor_type motors[MAX_MOTORS];


int PIN_TYPE_DIGITAL = 1;
int PIN_TYPE_ANALOG = 2;
int PIN_TYPE_PWM = 4;

// Pins
/*
typedef struct
{
	int pin; // number
	int rate;
	int type;
	int feedbackRate;
	long count;
	long targetCount;
	int value;

	// boolean to activate or deactivate a pin
	// this dominates all other state changes
	bool isActive;

	int method;

	// int step; // affects speed usually 1

	int min;
	int max;

	int increment;

	// event related
	bool eventsEnabled;
} pin_type;


pin_type pins[DIGITAL_PIN_COUNT]; // MAX_PINS - is this defined?

*/

unsigned long loopCount = 0;
unsigned long lastMicros = 0;
int byteCount = 0;
unsigned char newByte = 0;
unsigned char ioCmd[64];  // message buffer for all inbound messages
int readValue;

// FIXME - normalize with sampleRate ..
int loadTimingModulus = 1000;

boolean loadTimingEnabled = false;
unsigned long loadTime = 0;
// TODO - avg load time

unsigned int sampleRate = 1; // 1 - 65,535 modulus of the loopcount - allowing you to sample less

int digitalReadPin[DIGITAL_PIN_COUNT];        // array of pins to read from
int digitalReadPollingPinCount = 0;           // number of pins currently reading
int lastDigitalInputValue[DIGITAL_PIN_COUNT]; // array of last input values
bool digitalTriggerOnly = false;      // send data back only if its different

int analogReadPin[ANALOG_PIN_COUNT];          // array of pins to read from
int analogReadPollingPinCount = 0;            // number of pins currently reading
int lastAnalogInputValue[ANALOG_PIN_COUNT];   // array of last input values
bool analogTriggerOnly = false;         // send data back only if its different

										// meh - should probably have a  Pin struct :P
int activePins[DIGITAL_PIN_COUNT];        	// array of pins to Pulse from
long digitalPulseCount[DIGITAL_PIN_COUNT];      // current location in pulse train
long digitalPulseStop[DIGITAL_PIN_COUNT];       // location where pulse should stop
int activePinCount = 0;           		// number of pins currently Pulseing

										//---- data record definitions begin -----


										// TODO - all well and good .. but you dont control Servo's data (yet)
										// should have a struct for it too - contains all the data info you'd want to have
										// in a servo - same with stepper

// FIXME - REMOVE
// TYPE OF SENSOR (WITH CONTROL PULSE TO READ)
typedef struct
{
	int servoIndex; // id of servo in servos array
					//int servoPos; in servo
	int sensorIndex;
	int sweepMin;
	int sweepMax;
	int step;
	bool isActive; // needed ? - is combo of two
}  pingdar_type;

pingdar_type pingdars[PINGDARS_MAX];

//===custom msg interface begin===
byte customParams[256];
int paramBuffIndex;
int paramCnt;
//===custom msg interface end===

void sendServoEvent(servo_type& s, int eventType);
void sendStepperEvent(stepper_type& s, int eventType);
unsigned long getUltrasonicRange(sensor_type& sensor);
// void sendMsg ( int num, ... );

//---- data record definitions end -----


// ----------- send custom msg begin ---------------------
void append(const int& data) {
	++paramCnt;
	customParams[paramBuffIndex] = ARDUINO_TYPE_INT;
	customParams[++paramBuffIndex] = (byte)(data >> 8);
	customParams[++paramBuffIndex] = ((byte)data & 0xff);
	++paramBuffIndex;
}

void startMsg() {
}

void setup() {
	Serial.begin(57600);        // connect to the serial port

	softReset();

	// --VENDOR SETUP BEGIN--
	// --VENDOR SETUP END--
}


void sendMsg() {

	// unbox
	Serial.write(MAGIC_NUMBER);
	Serial.write(paramBuffIndex + 2); // = param buff size + FN + paramCnt
									  //Serial.write(2); // = param buff size + FN + paramCnt
	Serial.write(CUSTOM_MSG);
	Serial.write(paramCnt);

	for (int i = 0; i < paramBuffIndex; ++i) {
		Serial.write(customParams[i]);
	}

	paramCnt = 0;
	paramBuffIndex = 0;
}
// ----------- send custom msg end ---------------------


void softReset()
{
	for (int i = 0; i < MAX_SERVOS - 1; ++i)
	{
		servo_type& s = servos[i];
		s.speed = 100;
		if (s.servo != 0) {
			s.servo->detach();
		}
	}

	// FIXME - need a reset for all sensors

	// FIXME - need PIN_COUNT - type is an internal representation
	/*
	for (int j = 0; j < DIGITAL_PIN_COUNT - 1; ++j)
	{
		pins[j].pin = j;
		pins[j].pin = false;
		// if digital pin .. blah blah
		pinMode(j, OUTPUT);

	}
	*/

	activePinCount = 0;
	digitalReadPollingPinCount = 0;
	analogReadPollingPinCount = 0;
	loopCount = 0;

}

// sets frequency of pwm of analog
// FIXME - us ifdef appropriate uC which
// support these clocks TCCR0B
void setPWMFrequency(int address, int prescalar)
{
	int clearBits = 0x07;
	if (address == 0x25)
	{
		TCCR0B &= ~clearBits;
		TCCR0B |= prescalar;
	}
	else if (address == 0x2E)
	{
		TCCR1B &= ~clearBits;
		TCCR1B |= prescalar;
	}
	else if (address == 0xA1)
	{
		TCCR2B &= ~clearBits;
		TCCR2B |= prescalar;
	}

}

unsigned long toUnsignedLong(unsigned char* buffer, int start) {
	return ((buffer[start + 3] << 24) + (buffer[start + 2] << 16) + (buffer[start + 1] << 8) + buffer[start]);
}

/**
* checks the existance of the searched value in the array
* - good for not adding to a dynamic list of values if it
* already exists
*/
bool exists(int array[], int len, int searchValue) {
	for (int i = 0; i < len; ++i)
	{
		if (searchValue == array[i])
		{
			return true;
		}
	}
	return false;
}

/**
* adds new value to a pseudo dynamic array/list
* if successful - if value already exists on list
* sends back an error
*/
bool addNewValue(int array[], int& len, int addValue)
{
	if (!exists(array, len, addValue)) {
		array[len] = addValue;
		++len;
		return true;
	}
	else {
		sendError(ERROR_ALREADY_EXISTS);
		return false;
	}
}


bool removeAndShift(int array[], int& len, int removeValue)
{
	if (!exists(array, len, removeValue)) {
		sendError(ERROR_DOES_NOT_EXIST);
		return false;
	}

	int pos = -1;

	if (len == 0)
	{
		// "should" never happen
		// would be calling remove on an empty list
		// the error ERROR_DOES_NOT_EXIST - "should" be called
		return true;
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
		return true;
	}

	// if found somewhere else shift left
	if (pos < len && pos > -1)
	{
		for (int j = pos; j < len - 1; ++j)
		{
			array[j] = array[j + 1];
		}
		--len;
	}

	return true;
}

boolean getCommand()
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

void loop() {

	++loopCount;

	if (getCommand())
	{
		switch (ioCmd[0])
		{

		// ---------- system method pass-through begin -------------
		case DIGITAL_WRITE: {
			digitalWrite(ioCmd[1], ioCmd[2]);
			break;
		}

		case ANALOG_WRITE: {
			analogWrite(ioCmd[1], ioCmd[2]);
			break;
		}

		case PIN_MODE: {
			pinMode(ioCmd[1], ioCmd[2]);
			break;
		}
		// FIXME - add pulseIn and any other system methods
		// ---------- system method pass-through end -------------
		
		// -------- servo begin ------------------------
		// we can add a little functionality to servos
		// in the layer of the driver 
		// for example speed 
		case SERVO_ATTACH: {
			servo_type& s = servos[ioCmd[1]];
			s.index = ioCmd[1];
			if (s.servo == NULL) {
				s.servo = new Servo();
			}
			s.servo->attach(ioCmd[2]);
			s.step = 1;
			s.eventsEnabled = false;
			break;
		}

		case SERVO_SWEEP_START: {
			servo_type& s = servos[ioCmd[1]];
			s.min = ioCmd[2];
			s.max = ioCmd[3];
			s.step = ioCmd[4];
			s.isMoving = true;
			s.isSweeping = true;
			break;
		}

		case SERVO_SWEEP_STOP: {
			servo_type& s = servos[ioCmd[1]];
			s.isMoving = false;
			s.isSweeping = false;
			break;
		}

		case SERVO_WRITE: {
			servo_type& s = servos[ioCmd[1]];
			if (s.speed == 100 && s.servo != 0)// move at regular/full 100% speed
			{
				s.targetPos = ioCmd[2];
				s.currentPos = ioCmd[2];
				s.isMoving = false;
				s.servo->write(ioCmd[2]);
				if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_STOPPED);
			}
			else if (s.speed < 100 && s.speed > 0) {
				s.targetPos = ioCmd[2];
				s.isMoving = true;
			}
			break;
		}

		case PUBLISH_SERVO_EVENT: {
			servo_type& s = servos[ioCmd[1]];
			s.eventsEnabled = ioCmd[2];
			break;
		}

		case SERVO_WRITE_MICROSECONDS: {
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

		case SET_SERVO_SPEED: {
			// setting the speed of a servo
			servo_type& servo = servos[ioCmd[1]];
			servo.speed = ioCmd[2];
			break;
		}

		case SERVO_DETACH: {
			servo_type& s = servos[ioCmd[1]];
			if (s.servo != 0) {
				s.servo->detach();
			}
			break;
		}

		// -------- servo begin ------------------------

		case SET_LOAD_TIMING_ENABLED: {
			loadTimingEnabled = ioCmd[1];
			//loadTimingModulus = ioCmd[2];
			loadTimingModulus = 1000;
			break;
		}

		case SET_PWMFREQUENCY: {
			setPWMFrequency(ioCmd[1], ioCmd[2]);
			break;
		}

		// FIXME - convert all to type SENSOR_PIN
		case ANALOG_READ_POLLING_START: {
			int pin = ioCmd[1];
			addNewValue(analogReadPin, analogReadPollingPinCount, pin);
			break;
		}

		case ANALOG_READ_POLLING_STOP: {
			int pin = ioCmd[1];
			removeAndShift(analogReadPin, analogReadPollingPinCount, pin);
			break;
		}

		case DIGITAL_READ_POLLING_START: {
			int pin = ioCmd[1];
			addNewValue(digitalReadPin, digitalReadPollingPinCount, pin);
			break;
		}
	
		// this is the same as digitalWrite except
	    // we can keep track of the number of pulses
		case PULSE: {
			// get sensor from index
			sensor_type& sensor = sensors[ioCmd[1]];

			// FIXME - this has to unload a Long !!!
			sensor.count = 0;
			//sensor.target = toUnsignedLong(ioCmd, 2);// ioCmd[2];
			sensor.target = ((ioCmd[2] << 24) + (ioCmd[3] << 16) + (ioCmd[4] << 8) + ioCmd[5]);
			sensor.rate = ioCmd[6];
			sensor.feedbackRate = ioCmd[7];
			sensor.isActive = true;
			sensor.state = PUBLISH_SENSOR_DATA;

			//addNewValue(activePins, activePinCount, ioCmd[1]);
			break;
		}

		case PULSE_STOP: {

			sensor_type& sensor = sensors[ioCmd[1]];

			// FIXME - this has to unload a Long !!!
			sensor.state = PUBLISH_PULSE_STOP;

			//removeAndShift(activePins, activePinCount, ioCmd[1]);
			break;
		}

		case DIGITAL_READ_POLLING_STOP: {
			int pin = ioCmd[1];
			removeAndShift(digitalReadPin, digitalReadPollingPinCount, pin);
			break;
		}

	    // FIXME - these should just be attributes of the sensor
		case SET_TRIGGER: {
			// FIXME !!! - you need 1. a complete pin list !!!   analog & digital should be defined by attribute not
			// data structure !!!  if (pin.type == ??? if needed
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
			analogReadPin[analogReadPollingPinCount] = ioCmd[1]; // put on polling read list
			++analogReadPollingPinCount;
			break;
		}

		case SET_DEBOUNCE: {
			// default debounceDelay = 50;
			debounceDelay = ((ioCmd[1] << 8) + ioCmd[2]);
			break;
		}

		case SET_DIGITAL_TRIGGER_ONLY: {
			digitalTriggerOnly = ioCmd[1];
			break;
		}

		case SET_SERIAL_RATE:
		{
			Serial.end();
			delay(500);
			Serial.begin(ioCmd[1]);
			break;
		}

		case GET_VERSION: {
			Serial.write(MAGIC_NUMBER);
			Serial.write(2); // size
			Serial.write(PUBLISH_VERSION);
			Serial.write((byte)MRLCOMM_VERSION);
			break;
		}

		case SET_SAMPLE_RATE: {
			// 2 byte int - valid range 1-65,535
			sampleRate = (ioCmd[1] << 8) + ioCmd[2];
			if (sampleRate == 0)
			{
				sampleRate = 1;
			} // avoid /0 error - FIXME - time estimate param
			break;
		}

		case SOFT_RESET: {
			softReset();
			break;
		}

		case STEPPER_ATTACH: {
			stepper_type& stepper = steppers[ioCmd[1]];
			stepper.index = ioCmd[1];
			stepper.type = ioCmd[2];
			stepper.currentPos = 0;
			stepper.targetPos = 0;
			stepper.speed = 100;

			if (stepper.type == STEPPER_TYPE_SIMPLE) {
				stepper.dirPin = ioCmd[3]; // dir pin
				stepper.stepPin = ioCmd[4]; // step pin
				pinMode(stepper.dirPin, OUTPUT);
				pinMode(stepper.stepPin, OUTPUT);
			}
			else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

		case STEPPER_RESET: {
			stepper_type& stepper = steppers[ioCmd[1]];
			stepper.currentPos = 0;
			stepper.targetPos = 0;
			stepper.speed = 100;
			break;
		}

							/* absolute position - not relative */
		case STEPPER_MOVE_TO: {
			stepper_type& stepper = steppers[ioCmd[1]];
			if (stepper.type == STEPPER_TYPE_SIMPLE) {
				stepper.targetPos = stepper.currentPos + (ioCmd[2] << 8) + ioCmd[3];
				// relative position & direction
				if (stepper.targetPos < 0) {
					// direction
					digitalWrite(stepper.dirPin, 1);
				}
				else {
					digitalWrite(stepper.dirPin, 0);
				}
			}
			else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

		case STEPPER_STOP: {
			stepper_type& stepper = steppers[ioCmd[1]];
			if (stepper.type == STEPPER_TYPE_SIMPLE) {
				stepper.targetPos = stepper.currentPos;
				sendStepperEvent(stepper, STEPPER_EVENT_STOP);
			}
			else {
				sendError(ERROR_UNKOWN_CMD);
			}
			break;
		}

		// THIS WILL BE THE NEW BIG-KAHUNA
		case SENSOR_ATTACH: {
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.count = 0;
			sensor.target = 0;
			sensor.type = ioCmd[2];

			// initialize based on sensor type
			if (sensor.type == SENSOR_ULTRASONIC) {
				sensor.trigPin = ioCmd[3];
				sensor.echoPin = ioCmd[4];
				pinMode(sensor.trigPin, OUTPUT);
				pinMode(sensor.echoPin, INPUT);
				//sensor.ping = new NewPing(sensor.trigPin, sensor.echoPin, 100);
			}
			else if (sensor.type == SENSOR_PULSE) {
				
				sensor.pulsePin = ioCmd[3];
			}

			break;
		}

	    // FIXME - this is the same as DIGITAL PIN POLLING START
		case SENSOR_POLLING_START: {
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.isActive = true;

			// I'm used to ms - and would need to change some
			// interfaces if i was to support inbound longs
			//sensor.timeoutUS = ioCmd[2] * 1000;
			sensor.timeoutUS = 20000; // 20 ms
			sensor.state = ECHO_STATE_START;

			break;
		}

		// FIXME - this is the same as DIGITAL PIN POLLING STOP
		case SENSOR_POLLING_STOP: {
			int sensorIndex = ioCmd[1];
			sensor_type& sensor = sensors[sensorIndex];
			sensor.isActive = false;
			break;
		}

		case NOP: {
			// No Operation
			break;
		}

		default: {
			sendError(ERROR_UNKOWN_CMD);
			break;
		}
		} // end switch

		  // reset buffer
		memset(ioCmd, 0, sizeof(ioCmd));
		byteCount = 0;

	} // if getCommand()


	  //////////////////// oink //////////////////////////


	  // FIXME - this represents "active" pins reading or
	  // writing - its all you need ! ;)
	/*
	for (int i = 0; i < DIGITAL_PIN_COUNT; ++i)
	{

		// associative array of sorts
		// int pinIndex = activePins[i];
		pin_type& pin = pins[i];
		if (pin.isActive != true) {
			continue;
		}

		// TODO - implement - rate = modulo speed
		// if (loopCount%rate == 0) {

		// toggle pin state
		pin.value = (pin.value == 0) ? 1 : 0;

		// leading edge ... 0 to 1
		if (pin.value == 1) {
			pin.count++;
			if (pin.count >= pin.targetCount) {
				pin.method = PUBLISH_PULSE_STOP;
			}
		}

		// change state of pin
		digitalWrite(pin.pin, pin.value);


		// move counter/current position
		// see if feedback rate is valid
		// if time to send feedback do it
		// if (loopCount%feedbackRate == 0)
		// 0--to-->1 counting leading edge only
		// pin.method == PUBLISH_PULSE_PIN &&
		// stopped on the leading edge
		if (pin.method != PUBLISH_PULSE_STOP && pin.value == 1)
		{
			pin.count++;
			Serial.write(MAGIC_NUMBER);
			Serial.write(6); // size
			Serial.write(pin.method); // Serial.write(PUBLISH_PULSE);
			Serial.write(pin.pin);// Pin#
			Serial.write(pin.count >> 24); 	// MSB 
			Serial.write(pin.count >> 16); 	// MSB
			Serial.write(pin.count >> 8); 	// MSB
			Serial.write(pin.count & 0xff); 	// LSB

												// deactivate
												// lastDebounceTime[digitalReadPin[i]] = millis();
		}

		if (pin.method == PUBLISH_PULSE_STOP) {
			pin.isActive = false;
		}

	}
	*/

	//////////////////// oink oink //////////////////////////

	// all reads are affected by sample rate
	if (loopCount%sampleRate == 0) {
		// digital polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
		for (int i = 0; i < digitalReadPollingPinCount; ++i)
		{
			if (debounceDelay)
			{
				if (millis() - lastDebounceTime[digitalReadPin[i]] < debounceDelay)
				{
					continue;
				}
			}

			// FIXME !! - normalize with analog
			// read the pin
			readValue = digitalRead(digitalReadPin[i]);

			// if my value is different from last time  && config - send it
			if (lastDigitalInputValue[digitalReadPin[i]] != readValue || !digitalTriggerOnly)
			{
				Serial.write(MAGIC_NUMBER);
				Serial.write(4); // size
				Serial.write(PUBLISH_PIN);
				Serial.write(digitalReadPin[i]);// Pin#
				Serial.write(0); 	// MSB
				Serial.write(readValue); 	// LSB

				lastDebounceTime[digitalReadPin[i]] = millis();
			}

			// set the last input value of this pin
			lastDigitalInputValue[digitalReadPin[i]] = readValue;
		}


		// analog polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
		for (int i = 0; i < analogReadPollingPinCount; ++i)
		{
			// read the pin
			readValue = analogRead(analogReadPin[i]);

			// if my value is different from last time - send it
			if (lastAnalogInputValue[analogReadPin[i]] != readValue || !analogTriggerOnly) //TODO - SEND_DELTA_MIN_DIFF
			{
				//sendMsg(4, ANALOG_VALUE, analogReadPin[i], readValue >> 8, readValue & 0xff);

				Serial.write(MAGIC_NUMBER);
				Serial.write(4); //size
				Serial.write(PUBLISH_PIN);
				Serial.write(analogReadPin[i]);
				Serial.write(readValue >> 8);   // MSB
				Serial.write(readValue & 0xff);	// LSB

			}
			// set the last input value of this pin
			lastAnalogInputValue[analogReadPin[i]] = readValue;
		}
	}

	// update moving servos - send events if required
	for (int i = 0; i < MAX_SERVOS; ++i)
	{
		servo_type& s = servos[i];
		if (s.isMoving && s.servo != 0) {
			if (s.currentPos != s.targetPos)
			{
				// caclulate the appropriate modulus to drive
				// the servo to the next position
				// TODO - check for speed > 0 && speed < 100 - send ERROR back?
				int speedModulus = (100 - s.speed) * 10;
				if (loopCount % speedModulus == 0)
				{
					int increment = s.step * ((s.currentPos < s.targetPos) ? 1 : -1);
					// move the servo an increment
					s.currentPos = s.currentPos + increment;
					s.servo->write(s.currentPos);
					if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_POSITION_UPDATE);
				}
			}
			else {
				if (s.isSweeping) {
					if (s.targetPos == s.min) {
						s.targetPos = s.max;
					}
					else {
						s.targetPos = s.min;
					}
				}
				else {
					if (s.eventsEnabled) sendServoEvent(s, SERVO_EVENT_STOPPED);
					s.isMoving = false;
				}
			}
		}
	}

	unsigned long ts;

	// FIXME - optimize with only "active" sensors !!!
	for (int i = 0; i < SENSORS_MAX; ++i) {
		sensor_type& sensor = sensors[i];

		if (sensor.isActive != true) {
			continue;
		}

		switch (sensor.type)
		{

		case SENSOR_ULTRASONIC: {
			// we are running & have an ultrasonic (ping) sensor
			// check to see what state we  are in

			if (sensor.state == ECHO_STATE_START) {
				// trigPin prepare - start low for an
				// upcoming high pulse
				pinMode(sensor.trigPin, OUTPUT);
				digitalWrite(sensor.trigPin, LOW);

				// put the echopin into a high state
				// is this necessary ???
				pinMode(sensor.echoPin, OUTPUT);
				digitalWrite(sensor.echoPin, HIGH);

				ts = micros();
				if (ts - sensor.ts > 2) {
					sensor.ts = ts;
					sensor.state = ECHO_STATE_TRIG_PULSE_BEGIN;
				}
			}
			else if (sensor.state == ECHO_STATE_TRIG_PULSE_BEGIN) {

				// begin high pulse for at least 10 us
				pinMode(sensor.trigPin, OUTPUT);
				digitalWrite(sensor.trigPin, HIGH);

				ts = micros();
				if (ts - sensor.ts > 10) {
					sensor.ts = ts;
					sensor.state = ECHO_STATE_TRIG_PULSE_END;
				}
			}
			else if (sensor.state == ECHO_STATE_TRIG_PULSE_END) {
				// end of pulse
				pinMode(sensor.trigPin, OUTPUT);
				digitalWrite(sensor.trigPin, LOW);

				sensor.state = ECHO_STATE_MIN_PAUSE_PRE_LISTENING;
				sensor.ts = micros();
			}
			else if (sensor.state == ECHO_STATE_MIN_PAUSE_PRE_LISTENING) {

				ts = micros();
				if (ts - sensor.ts > 1500) {
					sensor.ts = ts;

					// putting echo pin into listen mode
					pinMode(sensor.echoPin, OUTPUT);
					digitalWrite(sensor.echoPin, HIGH);
					pinMode(sensor.echoPin, INPUT);

					sensor.state = ECHO_STATE_LISTENING;
				}

			}
			else if (sensor.state == ECHO_STATE_LISTENING) {
				// timeout or change states..
				int value = digitalRead(sensor.echoPin);
				ts = micros();

				if (value == LOW) {
					sensor.lastValue = ts - sensor.ts;
					sensor.ts = ts;
					sensor.state = ECHO_STATE_GOOD_RANGE;
				}
				else if (ts - sensor.ts > sensor.timeoutUS) {
					sensor.state = ECHO_STATE_TIMEOUT;
					sensor.ts = ts;
					sensor.lastValue = 0;
				}

			}
			else if (sensor.state == ECHO_STATE_GOOD_RANGE || sensor.state == ECHO_STATE_TIMEOUT) {
				Serial.write(MAGIC_NUMBER);
				Serial.write(6); // size 1 FN + 4 bytes of unsigned long
				Serial.write(PUBLISH_SENSOR_DATA);
				Serial.write(i);
				// write the long value out
				Serial.write((byte)(sensor.lastValue >> 24));
				Serial.write((byte)(sensor.lastValue >> 16));
				Serial.write((byte)(sensor.lastValue >> 8));
				Serial.write((byte)sensor.lastValue & 0xff);
				sensor.state = ECHO_STATE_START;
			} // end else if
			break;
		}

		// because sensor pulse & pulsing are so closely linked
        // the pulse will be handled here as well even if the
		// read data sent back on serial is disabled
		case SENSOR_PULSE: {

			// TODO - implement - rate = modulo speed
			// if (loopCount%rate == 0) {

			// toggle pin state
			sensor.lastValue = (sensor.lastValue == 0) ? 1 : 0;

			// leading edge ... 0 to 1
			if (sensor.lastValue == 1) {
				sensor.count++;
				if (sensor.count >= sensor.target) {
					sensor.state = PUBLISH_PULSE_STOP;
				}
			}

			// change state of pin
			digitalWrite(sensor.pulsePin, sensor.lastValue);


			// move counter/current position
			// see if feedback rate is valid
			// if time to send feedback do it
			// if (loopCount%feedbackRate == 0)
			// 0--to-->1 counting leading edge only
			// pin.method == PUBLISH_PULSE_PIN &&
			// stopped on the leading edge
			if (sensor.state != PUBLISH_PULSE_STOP && sensor.lastValue == 1)
			{
				Serial.write(MAGIC_NUMBER);
				Serial.write(6); // size
				Serial.write(sensor.state); // Serial.write(PUBLISH_PULSE);
				Serial.write(sensor.index);// Pin#
				Serial.write(sensor.count >> 24); 	// MSB zoddly
				Serial.write(sensor.count >> 16); 	// MSB
				Serial.write(sensor.count >> 8); 	// MSB
				Serial.write(sensor.count & 0xff); 	// LSB

													// deactivate
													// lastDebounceTime[digitalReadPin[i]] = millis();
			}

			if (sensor.state == PUBLISH_PULSE_STOP) {
				sensor.isActive = false;
			}

			Serial.write(MAGIC_NUMBER);
			Serial.write(6); // size
			Serial.write(sensor.state); // Serial.write(PUBLISH_PULSE);
			Serial.write(sensor.index);// Pin#
			Serial.write(sensor.count >> 24); 	// MSB zoddly
			Serial.write(sensor.count >> 16); 	// MSB
			Serial.write(sensor.count >> 8); 	// MSB
			Serial.write(sensor.count & 0xff); 	// LSB


			break;
		}

		default: {
			sendError(ERROR_UNKOWN_SENSOR);
			break;
		}
		}

	} // end for each sensor

	  // TODO - brake - speed - fractional stepping - other stepper types
	for (int i = 0; i < STEPPERS_MAX; ++i) {
		stepper_type& stepper = steppers[i];
		if (stepper.currentPos != stepper.targetPos) {
			if (stepper.type == STEPPER_TYPE_SIMPLE) {

				// direction is already set in initial STEPPER_MOVE

				if (stepper.currentPos < stepper.targetPos) {
					digitalWrite(stepper.stepPin, 1);
					delayMicroseconds(1); // :P should require another state? loop is ~106us min ?
					digitalWrite(stepper.stepPin, 0);
					stepper.currentPos++;
					sendStepperEvent(stepper, STEPPER_EVENT_STEP);

				}
				else if (stepper.currentPos > stepper.targetPos) {
					digitalWrite(stepper.stepPin, 1);
					delayMicroseconds(1); // :P should require another state? loop is ~106us min ?
					digitalWrite(stepper.stepPin, 0);
					stepper.currentPos--;
					sendStepperEvent(stepper, STEPPER_EVENT_STEP);
				}

				if (stepper.currentPos == stepper.targetPos) {
					sendStepperEvent(stepper, STEPPER_EVENT_STOP);
				}

			}
		}
	}

	// FIXME - fix overflow with getDiff() method !!!
	unsigned long now = micros();
	loadTime = now - lastMicros; // avg outside
	lastMicros = now;

	// report load time
	if (loadTimingEnabled && (loopCount%loadTimingModulus == 0)) {

		// send it
		Serial.write(MAGIC_NUMBER);
		Serial.write(5); // size 1 FN + 4 bytes of unsigned long
		Serial.write(PUBLISH_LOAD_TIMING_EVENT);
		// write the long value out
		Serial.write((byte)(loadTime >> 24));
		Serial.write((byte)(loadTime >> 16));
		Serial.write((byte)(loadTime >> 8));
		Serial.write((byte)loadTime & 0xff);
	}


} // end of big loop

unsigned long getUltrasonicRange(sensor_type& sensor) {

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

void sendServoEvent(servo_type& s, int eventType) {
	// check type of event - STOP vs CURRENT POS

	Serial.write(MAGIC_NUMBER);
	Serial.write(5); // size = 1 FN + 1 INDEX + 1 eventType + 1 curPos
	Serial.write(PUBLISH_SERVO_EVENT);
	Serial.write(s.index); // send my index
						   // write the long value out
	Serial.write(eventType);
	Serial.write(s.currentPos);
	Serial.write(s.targetPos);
}

void sendStepperEvent(stepper_type& s, int eventType) {
	// check type of event - STOP vs CURRENT POS

	Serial.write(MAGIC_NUMBER);
	Serial.write(5); // size = 1 FN + 1 INDEX + 1 eventType + 1 curPos
	Serial.write(PUBLISH_STEPPER_EVENT);
	Serial.write(s.index); // send my index
						   // write the long value out
	Serial.write(eventType);
	Serial.write(s.currentPos >> 8); // msb
	Serial.write(s.currentPos & 0xff); // lsb
}


void sendError(int type) {
	Serial.write(MAGIC_NUMBER);
	Serial.write(2); // size = 1 FN + 1 TYPE
	Serial.write(PUBLISH_MRLCOMM_ERROR);
	Serial.write(type);
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
Serial.write(msgBuf, num + 2);
return;
}
*/