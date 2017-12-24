package org.myrobotlab.arduino;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.myrobotlab.logging.Level;

import org.myrobotlab.arduino.virtual.MrlComm;

/**
 * <pre>
 * 
 Welcome to Msg.java
 Its created by running ArduinoMsgGenerator
 which combines the MrlComm message schema (src/resource/Arduino/arduinoMsg.schema)
 with the cpp template (src/resource/Arduino/generate/Msg.template.java)

 	Schema Type Conversions

	Schema      ARDUINO					Java							Range
	none		byte/unsigned char		int (cuz Java byte bites)		1 byte - 0 to 255
	boolean		boolean					boolean							0 1
    b16			int						int (short)						2 bytes	-32,768 to 32,767
    b32			long					int								4 bytes -2,147,483,648 to 2,147,483, 647
    bu32		unsigned long			long							0 to 4,294,967,295
    str			char*, size				String							variable length
    []			byte[], size			int[]							variable length

 All message editing should be done in the arduinoMsg.schema

 The binary wire format of an MrlComm is:

 MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...
 
 </pre>

 */

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.VirtualArduino;

import java.io.FileOutputStream;
import java.util.Arrays;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * Singlton messaging interface to an MrlComm
 *
 * @author GroG
 *
 */

public class VirtualMsg {

	public static final int MAX_MSG_SIZE = 64;
	public static final int MAGIC_NUMBER = 170; // 10101010
	public static final int MRLCOMM_VERSION = 57;
	
	// send buffer
  int sendBufferSize = 0;
  int sendBuffer[] = new int[MAX_MSG_SIZE];
  
  // recv buffer
  int ioCmd[] = new int[MAX_MSG_SIZE];
  
  int byteCount = 0;
  int msgSize = 0;

	// ------ device type mapping constants
	int method = -1;
	public boolean debug = false;
	boolean invoke = true;
	
	boolean ackEnabled = false;
	
	 public static class AckLock {
	    // first is always true - since there
	    // is no msg to be acknowledged...
	    volatile boolean acknowledged = true;
	  }
	 
	transient AckLock ackRecievedLock = new AckLock();
	
	// recording related
	transient FileOutputStream record = null;
	transient StringBuilder rxBuffer = new StringBuilder();
	transient StringBuilder txBuffer = new StringBuilder();	

	public static final int DEVICE_TYPE_UNKNOWN	 = 		0;
	public static final int DEVICE_TYPE_ARDUINO	 = 		1;
	public static final int DEVICE_TYPE_ULTRASONICSENSOR	 = 		2;
	public static final int DEVICE_TYPE_STEPPER	 = 		3;
	public static final int DEVICE_TYPE_MOTOR	 = 		4;
	public static final int DEVICE_TYPE_SERVO	 = 		5;
	public static final int DEVICE_TYPE_SERIAL	 = 		6;
	public static final int DEVICE_TYPE_I2C	 = 		7;
	public static final int DEVICE_TYPE_NEOPIXEL	 = 		8;
		
	// < publishMRLCommError/str errorMsg
	public final static int PUBLISH_MRLCOMM_ERROR = 1;
	// > getBoardInfo
	public final static int GET_BOARD_INFO = 2;
	// < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/activePins/[] deviceSummary
	public final static int PUBLISH_BOARD_INFO = 3;
	// > enablePin/address/type/b16 rate
	public final static int ENABLE_PIN = 4;
	// > setDebug/bool enabled
	public final static int SET_DEBUG = 5;
	// > setSerialRate/b32 rate
	public final static int SET_SERIAL_RATE = 6;
	// > softReset
	public final static int SOFT_RESET = 7;
	// > enableAck/bool enabled
	public final static int ENABLE_ACK = 8;
	// < publishAck/function
	public final static int PUBLISH_ACK = 9;
	// > echo/f32 myFloat/myByte/f32 secondFloat
	public final static int ECHO = 10;
	// < publishEcho/f32 myFloat/myByte/f32 secondFloat
	public final static int PUBLISH_ECHO = 11;
	// > customMsg/[] msg
	public final static int CUSTOM_MSG = 12;
	// < publishCustomMsg/[] msg
	public final static int PUBLISH_CUSTOM_MSG = 13;
	// > deviceDetach/deviceId
	public final static int DEVICE_DETACH = 14;
	// > i2cBusAttach/deviceId/i2cBus
	public final static int I2C_BUS_ATTACH = 15;
	// > i2cRead/deviceId/deviceAddress/size
	public final static int I2C_READ = 16;
	// > i2cWrite/deviceId/deviceAddress/[] data
	public final static int I2C_WRITE = 17;
	// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
	public final static int I2C_WRITE_READ = 18;
	// < publishI2cData/deviceId/[] data
	public final static int PUBLISH_I2C_DATA = 19;
	// > neoPixelAttach/deviceId/pin/b32 numPixels
	public final static int NEO_PIXEL_ATTACH = 20;
	// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
	public final static int NEO_PIXEL_SET_ANIMATION = 21;
	// > neoPixelWriteMatrix/deviceId/[] buffer
	public final static int NEO_PIXEL_WRITE_MATRIX = 22;
	// > analogWrite/pin/value
	public final static int ANALOG_WRITE = 23;
	// > digitalWrite/pin/value
	public final static int DIGITAL_WRITE = 24;
	// > disablePin/pin
	public final static int DISABLE_PIN = 25;
	// > disablePins
	public final static int DISABLE_PINS = 26;
	// > pinMode/pin/mode
	public final static int PIN_MODE = 27;
	// < publishDebug/str debugMsg
	public final static int PUBLISH_DEBUG = 28;
	// < publishPinArray/[] data
	public final static int PUBLISH_PIN_ARRAY = 29;
	// > setTrigger/pin/triggerValue
	public final static int SET_TRIGGER = 30;
	// > setDebounce/pin/delay
	public final static int SET_DEBOUNCE = 31;
	// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
	public final static int SERVO_ATTACH = 32;
	// > servoAttachPin/deviceId/pin
	public final static int SERVO_ATTACH_PIN = 33;
	// > servoDetachPin/deviceId
	public final static int SERVO_DETACH_PIN = 34;
	// > servoSetVelocity/deviceId/b16 velocity
	public final static int SERVO_SET_VELOCITY = 35;
	// > servoSweepStart/deviceId/min/max/step
	public final static int SERVO_SWEEP_START = 36;
	// > servoSweepStop/deviceId
	public final static int SERVO_SWEEP_STOP = 37;
	// > servoMoveToMicroseconds/deviceId/b16 target
	public final static int SERVO_MOVE_TO_MICROSECONDS = 38;
	// > servoSetAcceleration/deviceId/b16 acceleration
	public final static int SERVO_SET_ACCELERATION = 39;
	// < publishServoEvent/deviceId/eventType/b16 currentPos/b16 targetPos
	public final static int PUBLISH_SERVO_EVENT = 40;
	// > serialAttach/deviceId/relayPin
	public final static int SERIAL_ATTACH = 41;
	// > serialRelay/deviceId/[] data
	public final static int SERIAL_RELAY = 42;
	// < publishSerialData/deviceId/[] data
	public final static int PUBLISH_SERIAL_DATA = 43;
	// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
	public final static int ULTRASONIC_SENSOR_ATTACH = 44;
	// > ultrasonicSensorStartRanging/deviceId
	public final static int ULTRASONIC_SENSOR_START_RANGING = 45;
	// > ultrasonicSensorStopRanging/deviceId
	public final static int ULTRASONIC_SENSOR_STOP_RANGING = 46;
	// < publishUltrasonicSensorData/deviceId/b16 echoTime
	public final static int PUBLISH_ULTRASONIC_SENSOR_DATA = 47;
	// > setAref/b16 type
	public final static int SET_AREF = 48;
	// > motorAttach/deviceId/type/[] pins
	public final static int MOTOR_ATTACH = 49;
	// > motorMove/deviceId/pwr
	public final static int MOTOR_MOVE = 50;
	// > motorMoveTo/deviceId/pos
	public final static int MOTOR_MOVE_TO = 51;


/**
 * These methods will be invoked from the Msg class as callbacks from MrlComm.
 */
	
	// public void getBoardInfo(){}
	// public void enablePin(Integer address/*byte*/, Integer type/*byte*/, Integer rate/*b16*/){}
	// public void setDebug(Boolean enabled/*bool*/){}
	// public void setSerialRate(Integer rate/*b32*/){}
	// public void softReset(){}
	// public void enableAck(Boolean enabled/*bool*/){}
	// public void echo(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/){}
	// public void customMsg(int[] msg/*[]*/){}
	// public void deviceDetach(Integer deviceId/*byte*/){}
	// public void i2cBusAttach(Integer deviceId/*byte*/, Integer i2cBus/*byte*/){}
	// public void i2cRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer size/*byte*/){}
	// public void i2cWrite(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, int[] data/*[]*/){}
	// public void i2cWriteRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer readSize/*byte*/, Integer writeValue/*byte*/){}
	// public void neoPixelAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer numPixels/*b32*/){}
	// public void neoPixelSetAnimation(Integer deviceId/*byte*/, Integer animation/*byte*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer speed/*b16*/){}
	// public void neoPixelWriteMatrix(Integer deviceId/*byte*/, int[] buffer/*[]*/){}
	// public void analogWrite(Integer pin/*byte*/, Integer value/*byte*/){}
	// public void digitalWrite(Integer pin/*byte*/, Integer value/*byte*/){}
	// public void disablePin(Integer pin/*byte*/){}
	// public void disablePins(){}
	// public void pinMode(Integer pin/*byte*/, Integer mode/*byte*/){}
	// public void setTrigger(Integer pin/*byte*/, Integer triggerValue/*byte*/){}
	// public void setDebounce(Integer pin/*byte*/, Integer delay/*byte*/){}
	// public void servoAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer initPos/*b16*/, Integer initVelocity/*b16*/, String name/*str*/){}
	// public void servoAttachPin(Integer deviceId/*byte*/, Integer pin/*byte*/){}
	// public void servoDetachPin(Integer deviceId/*byte*/){}
	// public void servoSetVelocity(Integer deviceId/*byte*/, Integer velocity/*b16*/){}
	// public void servoSweepStart(Integer deviceId/*byte*/, Integer min/*byte*/, Integer max/*byte*/, Integer step/*byte*/){}
	// public void servoSweepStop(Integer deviceId/*byte*/){}
	// public void servoMoveToMicroseconds(Integer deviceId/*byte*/, Integer target/*b16*/){}
	// public void servoSetAcceleration(Integer deviceId/*byte*/, Integer acceleration/*b16*/){}
	// public void serialAttach(Integer deviceId/*byte*/, Integer relayPin/*byte*/){}
	// public void serialRelay(Integer deviceId/*byte*/, int[] data/*[]*/){}
	// public void ultrasonicSensorAttach(Integer deviceId/*byte*/, Integer triggerPin/*byte*/, Integer echoPin/*byte*/){}
	// public void ultrasonicSensorStartRanging(Integer deviceId/*byte*/){}
	// public void ultrasonicSensorStopRanging(Integer deviceId/*byte*/){}
	// public void setAref(Integer type/*b16*/){}
	// public void motorAttach(Integer deviceId/*byte*/, Integer type/*byte*/, int[] pins/*[]*/){}
	// public void motorMove(Integer deviceId/*byte*/, Integer pwr/*byte*/){}
	// public void motorMoveTo(Integer deviceId/*byte*/, Integer pos/*byte*/){}
	

	
	public transient final static Logger log = LoggerFactory.getLogger(Msg.class);

	public VirtualMsg(MrlComm arduino, SerialDevice serial) {
		this.arduino = arduino;
		this.serial = serial;
	}
	
	public void begin(SerialDevice serial){
	  this.serial = serial;
	}

	// transient private Msg instance;

	// ArduinoSerialCallBacks - TODO - extract interface
	transient private MrlComm arduino;
	
	transient private SerialDevice serial;

	/**
	 * want to grab it when SerialDevice is created
	 *
	 * @param serial
	 * @return
	 */
	/*
	static public synchronized Msg getInstance(MrlComm arduino, SerialDevice serial) {
		if (instance == null) {
			instance = new Msg();
		}

		instance.arduino = arduino;
		instance.serial = serial;

		return instance;
	}
	*/
	
	public void setInvoke(boolean b){
	  invoke = b;
	}
	
	public void processCommand(){
	  processCommand(ioCmd);
	}
	
	public void processCommand(int[] ioCmd) {
		int startPos = 0;
		method = ioCmd[startPos];
		switch (method) {
		case GET_BOARD_INFO: {
			if(invoke){
				arduino.invoke("getBoardInfo");
			} else { 
 				arduino.getBoardInfo();
			}
			break;
		}
		case ENABLE_PIN: {
			Integer address = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer type = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer rate = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("enablePin",  address,  type,  rate);
			} else { 
 				arduino.enablePin( address,  type,  rate);
			}
			break;
		}
		case SET_DEBUG: {
			Boolean enabled = (ioCmd[startPos+1] == 0)?false:true;
			startPos += 1;
			if(invoke){
				arduino.invoke("setDebug",  enabled);
			} else { 
 				arduino.setDebug( enabled);
			}
			break;
		}
		case SET_SERIAL_RATE: {
			Integer rate = b32(ioCmd, startPos+1);
			startPos += 4; //b32
			if(invoke){
				arduino.invoke("setSerialRate",  rate);
			} else { 
 				arduino.setSerialRate( rate);
			}
			break;
		}
		case SOFT_RESET: {
			if(invoke){
				arduino.invoke("softReset");
			} else { 
 				arduino.softReset();
			}
			break;
		}
		case ENABLE_ACK: {
			Boolean enabled = (ioCmd[startPos+1] == 0)?false:true;
			startPos += 1;
			if(invoke){
				arduino.invoke("enableAck",  enabled);
			} else { 
 				arduino.enableAck( enabled);
			}
			break;
		}
		case ECHO: {
			Float myFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			Integer myByte = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Float secondFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			if(invoke){
				arduino.invoke("echo",  myFloat,  myByte,  secondFloat);
			} else { 
 				arduino.echo( myFloat,  myByte,  secondFloat);
			}
			break;
		}
		case CUSTOM_MSG: {
			int[] msg = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("customMsg",  msg);
			} else { 
 				arduino.customMsg( msg);
			}
			break;
		}
		case DEVICE_DETACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("deviceDetach",  deviceId);
			} else { 
 				arduino.deviceDetach( deviceId);
			}
			break;
		}
		case I2C_BUS_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer i2cBus = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("i2cBusAttach",  deviceId,  i2cBus);
			} else { 
 				arduino.i2cBusAttach( deviceId,  i2cBus);
			}
			break;
		}
		case I2C_READ: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer size = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("i2cRead",  deviceId,  deviceAddress,  size);
			} else { 
 				arduino.i2cRead( deviceId,  deviceAddress,  size);
			}
			break;
		}
		case I2C_WRITE: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("i2cWrite",  deviceId,  deviceAddress,  data);
			} else { 
 				arduino.i2cWrite( deviceId,  deviceAddress,  data);
			}
			break;
		}
		case I2C_WRITE_READ: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer deviceAddress = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer readSize = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer writeValue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("i2cWriteRead",  deviceId,  deviceAddress,  readSize,  writeValue);
			} else { 
 				arduino.i2cWriteRead( deviceId,  deviceAddress,  readSize,  writeValue);
			}
			break;
		}
		case NEO_PIXEL_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer numPixels = b32(ioCmd, startPos+1);
			startPos += 4; //b32
			if(invoke){
				arduino.invoke("neoPixelAttach",  deviceId,  pin,  numPixels);
			} else { 
 				arduino.neoPixelAttach( deviceId,  pin,  numPixels);
			}
			break;
		}
		case NEO_PIXEL_SET_ANIMATION: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer animation = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer red = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer green = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer blue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer speed = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("neoPixelSetAnimation",  deviceId,  animation,  red,  green,  blue,  speed);
			} else { 
 				arduino.neoPixelSetAnimation( deviceId,  animation,  red,  green,  blue,  speed);
			}
			break;
		}
		case NEO_PIXEL_WRITE_MATRIX: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] buffer = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("neoPixelWriteMatrix",  deviceId,  buffer);
			} else { 
 				arduino.neoPixelWriteMatrix( deviceId,  buffer);
			}
			break;
		}
		case ANALOG_WRITE: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer value = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("analogWrite",  pin,  value);
			} else { 
 				arduino.analogWrite( pin,  value);
			}
			break;
		}
		case DIGITAL_WRITE: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer value = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("digitalWrite",  pin,  value);
			} else { 
 				arduino.digitalWrite( pin,  value);
			}
			break;
		}
		case DISABLE_PIN: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("disablePin",  pin);
			} else { 
 				arduino.disablePin( pin);
			}
			break;
		}
		case DISABLE_PINS: {
			if(invoke){
				arduino.invoke("disablePins");
			} else { 
 				arduino.disablePins();
			}
			break;
		}
		case PIN_MODE: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer mode = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("pinMode",  pin,  mode);
			} else { 
 				arduino.pinMode( pin,  mode);
			}
			break;
		}
		case SET_TRIGGER: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer triggerValue = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("setTrigger",  pin,  triggerValue);
			} else { 
 				arduino.setTrigger( pin,  triggerValue);
			}
			break;
		}
		case SET_DEBOUNCE: {
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer delay = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("setDebounce",  pin,  delay);
			} else { 
 				arduino.setDebounce( pin,  delay);
			}
			break;
		}
		case SERVO_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer initPos = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			Integer initVelocity = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			String name = str(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("servoAttach",  deviceId,  pin,  initPos,  initVelocity,  name);
			} else { 
 				arduino.servoAttach( deviceId,  pin,  initPos,  initVelocity,  name);
			}
			break;
		}
		case SERVO_ATTACH_PIN: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer pin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("servoAttachPin",  deviceId,  pin);
			} else { 
 				arduino.servoAttachPin( deviceId,  pin);
			}
			break;
		}
		case SERVO_DETACH_PIN: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("servoDetachPin",  deviceId);
			} else { 
 				arduino.servoDetachPin( deviceId);
			}
			break;
		}
		case SERVO_SET_VELOCITY: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer velocity = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("servoSetVelocity",  deviceId,  velocity);
			} else { 
 				arduino.servoSetVelocity( deviceId,  velocity);
			}
			break;
		}
		case SERVO_SWEEP_START: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer min = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer max = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer step = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("servoSweepStart",  deviceId,  min,  max,  step);
			} else { 
 				arduino.servoSweepStart( deviceId,  min,  max,  step);
			}
			break;
		}
		case SERVO_SWEEP_STOP: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("servoSweepStop",  deviceId);
			} else { 
 				arduino.servoSweepStop( deviceId);
			}
			break;
		}
		case SERVO_MOVE_TO_MICROSECONDS: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer target = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("servoMoveToMicroseconds",  deviceId,  target);
			} else { 
 				arduino.servoMoveToMicroseconds( deviceId,  target);
			}
			break;
		}
		case SERVO_SET_ACCELERATION: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer acceleration = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("servoSetAcceleration",  deviceId,  acceleration);
			} else { 
 				arduino.servoSetAcceleration( deviceId,  acceleration);
			}
			break;
		}
		case SERIAL_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer relayPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("serialAttach",  deviceId,  relayPin);
			} else { 
 				arduino.serialAttach( deviceId,  relayPin);
			}
			break;
		}
		case SERIAL_RELAY: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("serialRelay",  deviceId,  data);
			} else { 
 				arduino.serialRelay( deviceId,  data);
			}
			break;
		}
		case ULTRASONIC_SENSOR_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer triggerPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer echoPin = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("ultrasonicSensorAttach",  deviceId,  triggerPin,  echoPin);
			} else { 
 				arduino.ultrasonicSensorAttach( deviceId,  triggerPin,  echoPin);
			}
			break;
		}
		case ULTRASONIC_SENSOR_START_RANGING: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("ultrasonicSensorStartRanging",  deviceId);
			} else { 
 				arduino.ultrasonicSensorStartRanging( deviceId);
			}
			break;
		}
		case ULTRASONIC_SENSOR_STOP_RANGING: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("ultrasonicSensorStopRanging",  deviceId);
			} else { 
 				arduino.ultrasonicSensorStopRanging( deviceId);
			}
			break;
		}
		case SET_AREF: {
			Integer type = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("setAref",  type);
			} else { 
 				arduino.setAref( type);
			}
			break;
		}
		case MOTOR_ATTACH: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer type = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] pins = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("motorAttach",  deviceId,  type,  pins);
			} else { 
 				arduino.motorAttach( deviceId,  type,  pins);
			}
			break;
		}
		case MOTOR_MOVE: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer pwr = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("motorMove",  deviceId,  pwr);
			} else { 
 				arduino.motorMove( deviceId,  pwr);
			}
			break;
		}
		case MOTOR_MOVE_TO: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer pos = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("motorMoveTo",  deviceId,  pos);
			} else { 
 				arduino.motorMoveTo( deviceId,  pos);
			}
			break;
		}
		
		}
	}
	

	// Java-land --to--> MrlComm

	public synchronized void publishMRLCommError(String errorMsg/*str*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + (1 + errorMsg.length())); // size
			write(PUBLISH_MRLCOMM_ERROR); // msgType = 1
			write(errorMsg);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishMRLCommError");
				txBuffer.append("/");
				txBuffer.append(errorMsg);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishMRLCommError threw",e);
	  }
	}

	public synchronized void publishBoardInfo(Integer version/*byte*/, Integer boardType/*byte*/, Integer microsPerLoop/*b16*/, Integer sram/*b16*/, Integer activePins/*byte*/, int[] deviceSummary/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 2 + 2 + 1 + (1 + deviceSummary.length)); // size
			write(PUBLISH_BOARD_INFO); // msgType = 3
			write(version);
			write(boardType);
			writeb16(microsPerLoop);
			writeb16(sram);
			write(activePins);
			write(deviceSummary);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishBoardInfo");
				txBuffer.append("/");
				txBuffer.append(version);
				txBuffer.append("/");
				txBuffer.append(boardType);
				txBuffer.append("/");
				txBuffer.append(microsPerLoop);
				txBuffer.append("/");
				txBuffer.append(sram);
				txBuffer.append("/");
				txBuffer.append(activePins);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(deviceSummary));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishBoardInfo threw",e);
	  }
	}

	public synchronized void publishAck(Integer function/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(PUBLISH_ACK); // msgType = 9
			write(function);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishAck");
				txBuffer.append("/");
				txBuffer.append(function);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishAck threw",e);
	  }
	}

	public synchronized void publishEcho(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 4 + 1 + 4); // size
			write(PUBLISH_ECHO); // msgType = 11
			writef32(myFloat);
			write(myByte);
			writef32(secondFloat);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishEcho");
				txBuffer.append("/");
				txBuffer.append(myFloat);
				txBuffer.append("/");
				txBuffer.append(myByte);
				txBuffer.append("/");
				txBuffer.append(secondFloat);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishEcho threw",e);
	  }
	}

	public synchronized void publishCustomMsg(int[] msg/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + (1 + msg.length)); // size
			write(PUBLISH_CUSTOM_MSG); // msgType = 13
			write(msg);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishCustomMsg");
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(msg));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishCustomMsg threw",e);
	  }
	}

	public synchronized void publishI2cData(Integer deviceId/*byte*/, int[] data/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + (1 + data.length)); // size
			write(PUBLISH_I2C_DATA); // msgType = 19
			write(deviceId);
			write(data);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishI2cData");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(data));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishI2cData threw",e);
	  }
	}

	public synchronized void publishDebug(String debugMsg/*str*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + (1 + debugMsg.length())); // size
			write(PUBLISH_DEBUG); // msgType = 28
			write(debugMsg);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishDebug");
				txBuffer.append("/");
				txBuffer.append(debugMsg);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishDebug threw",e);
	  }
	}

	public synchronized void publishPinArray(int[] data/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + (1 + data.length)); // size
			write(PUBLISH_PIN_ARRAY); // msgType = 29
			write(data);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishPinArray");
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(data));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishPinArray threw",e);
	  }
	}

	public synchronized void publishServoEvent(Integer deviceId/*byte*/, Integer eventType/*byte*/, Integer currentPos/*b16*/, Integer targetPos/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 2 + 2); // size
			write(PUBLISH_SERVO_EVENT); // msgType = 40
			write(deviceId);
			write(eventType);
			writeb16(currentPos);
			writeb16(targetPos);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishServoEvent");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(eventType);
				txBuffer.append("/");
				txBuffer.append(currentPos);
				txBuffer.append("/");
				txBuffer.append(targetPos);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishServoEvent threw",e);
	  }
	}

	public synchronized void publishSerialData(Integer deviceId/*byte*/, int[] data/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + (1 + data.length)); // size
			write(PUBLISH_SERIAL_DATA); // msgType = 43
			write(deviceId);
			write(data);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishSerialData");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(data));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishSerialData threw",e);
	  }
	}

	public synchronized void publishUltrasonicSensorData(Integer deviceId/*byte*/, Integer echoTime/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 2); // size
			write(PUBLISH_ULTRASONIC_SENSOR_DATA); // msgType = 47
			write(deviceId);
			writeb16(echoTime);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> publishUltrasonicSensorData");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(echoTime);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("publishUltrasonicSensorData threw",e);
	  }
	}


	public static String methodToString(int method) {
		switch (method) {
		case PUBLISH_MRLCOMM_ERROR:{
			return "publishMRLCommError";
		}
		case GET_BOARD_INFO:{
			return "getBoardInfo";
		}
		case PUBLISH_BOARD_INFO:{
			return "publishBoardInfo";
		}
		case ENABLE_PIN:{
			return "enablePin";
		}
		case SET_DEBUG:{
			return "setDebug";
		}
		case SET_SERIAL_RATE:{
			return "setSerialRate";
		}
		case SOFT_RESET:{
			return "softReset";
		}
		case ENABLE_ACK:{
			return "enableAck";
		}
		case PUBLISH_ACK:{
			return "publishAck";
		}
		case ECHO:{
			return "echo";
		}
		case PUBLISH_ECHO:{
			return "publishEcho";
		}
		case CUSTOM_MSG:{
			return "customMsg";
		}
		case PUBLISH_CUSTOM_MSG:{
			return "publishCustomMsg";
		}
		case DEVICE_DETACH:{
			return "deviceDetach";
		}
		case I2C_BUS_ATTACH:{
			return "i2cBusAttach";
		}
		case I2C_READ:{
			return "i2cRead";
		}
		case I2C_WRITE:{
			return "i2cWrite";
		}
		case I2C_WRITE_READ:{
			return "i2cWriteRead";
		}
		case PUBLISH_I2C_DATA:{
			return "publishI2cData";
		}
		case NEO_PIXEL_ATTACH:{
			return "neoPixelAttach";
		}
		case NEO_PIXEL_SET_ANIMATION:{
			return "neoPixelSetAnimation";
		}
		case NEO_PIXEL_WRITE_MATRIX:{
			return "neoPixelWriteMatrix";
		}
		case ANALOG_WRITE:{
			return "analogWrite";
		}
		case DIGITAL_WRITE:{
			return "digitalWrite";
		}
		case DISABLE_PIN:{
			return "disablePin";
		}
		case DISABLE_PINS:{
			return "disablePins";
		}
		case PIN_MODE:{
			return "pinMode";
		}
		case PUBLISH_DEBUG:{
			return "publishDebug";
		}
		case PUBLISH_PIN_ARRAY:{
			return "publishPinArray";
		}
		case SET_TRIGGER:{
			return "setTrigger";
		}
		case SET_DEBOUNCE:{
			return "setDebounce";
		}
		case SERVO_ATTACH:{
			return "servoAttach";
		}
		case SERVO_ATTACH_PIN:{
			return "servoAttachPin";
		}
		case SERVO_DETACH_PIN:{
			return "servoDetachPin";
		}
		case SERVO_SET_VELOCITY:{
			return "servoSetVelocity";
		}
		case SERVO_SWEEP_START:{
			return "servoSweepStart";
		}
		case SERVO_SWEEP_STOP:{
			return "servoSweepStop";
		}
		case SERVO_MOVE_TO_MICROSECONDS:{
			return "servoMoveToMicroseconds";
		}
		case SERVO_SET_ACCELERATION:{
			return "servoSetAcceleration";
		}
		case PUBLISH_SERVO_EVENT:{
			return "publishServoEvent";
		}
		case SERIAL_ATTACH:{
			return "serialAttach";
		}
		case SERIAL_RELAY:{
			return "serialRelay";
		}
		case PUBLISH_SERIAL_DATA:{
			return "publishSerialData";
		}
		case ULTRASONIC_SENSOR_ATTACH:{
			return "ultrasonicSensorAttach";
		}
		case ULTRASONIC_SENSOR_START_RANGING:{
			return "ultrasonicSensorStartRanging";
		}
		case ULTRASONIC_SENSOR_STOP_RANGING:{
			return "ultrasonicSensorStopRanging";
		}
		case PUBLISH_ULTRASONIC_SENSOR_DATA:{
			return "publishUltrasonicSensorData";
		}
		case SET_AREF:{
			return "setAref";
		}
		case MOTOR_ATTACH:{
			return "motorAttach";
		}
		case MOTOR_MOVE:{
			return "motorMove";
		}
		case MOTOR_MOVE_TO:{
			return "motorMoveTo";
		}

		default: {
			return "ERROR UNKNOWN METHOD (" + Integer.toString(method) + ")";

		} // default
		}
	}

	public String str(int[] buffer, int start, int size) {
		byte[] b = new byte[size];
		for (int i = start; i < start + size; ++i){
			b[i - start] = (byte)(buffer[i] & 0xFF);
		}
		return new String(b);
	}

	public int[] subArray(int[] buffer, int start, int size) {		
		return Arrays.copyOfRange(buffer, start, start + size);
	}

	// signed 16 bit bucket
	public int b16(int[] buffer, int start/*=0*/) {
		return  (short)(buffer[start] << 8) + buffer[start + 1];
	}
	
	// signed 32 bit bucket
	public int b32(int[] buffer, int start/*=0*/) {
		return ((buffer[start + 0] << 24) + (buffer[start + 1] << 16)
				+ (buffer[start + 2] << 8) + buffer[start + 3]);
	}
	
	// unsigned 32 bit bucket
	public long bu32(int[] buffer, int start/*=0*/) {
		long ret = ((buffer[start + 0] << 24)
				+ (buffer[start + 1] << 16)
				+ (buffer[start + 2] << 8) + buffer[start + 3]);
		if (ret < 0){
			return 4294967296L + ret;
		}
		
		return ret;
	}

  // float 32 bit bucket
  public float f32(int[] buffer, int start/*=0*/) {
    byte[] b = new byte[4];
    for (int i = 0; i < 4; ++i){
      b[i] = (byte)buffer[start + i];
    }
    float f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
    return f;
  }
  
  public boolean readMsg() throws Exception {
    // handle serial data begin
    int bytesAvailable = serial.available();
    if (bytesAvailable > 0) {
      //publishDebug("RXBUFF:" + String(bytesAvailable));
      // now we should loop over the available bytes .. not just read one by one.
      for (int i = 0; i < bytesAvailable; i++) {
        // read the incoming byte:
        int newByte = serial.read();
        //publishDebug("RX:" + String(newByte));
        ++byteCount;
        // checking first byte - beginning of message?
        if (byteCount == 1 && newByte != VirtualMsg.MAGIC_NUMBER) {
          publishError(F("error serial"));
          // reset - try again
          byteCount = 0;
          // return false;
        }
        if (byteCount == 2) {
          // get the size of message
          // todo check msg < 64 (MAX_MSG_SIZE)
          if (newByte > 64) {
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
          byteCount = 0;
          return true;
        }
      }
    } // if Serial.available
      // we only partially read a command.  (or nothing at all.)
    return false;
  }

  String F(String msg) {
    return msg;
  }
  
  public void publishError(String error) {
    log.error(error);
  }
  
	void write(int b8) throws Exception {

		if ((b8 < 0) || (b8 > 255)) {
			log.error("writeByte overrun - should be  0 <= value <= 255 - value = {}", b8);
		}

		serial.write(b8 & 0xFF);
	}

	void writebool(boolean b1) throws Exception {
		if (b1) {
			serial.write(1);
		} else {
			serial.write(0);
		}
	}

	void writeb16(int b16) throws Exception {
		if ((b16 < -32768) || (b16 > 32767)) {
			log.error("writeByte overrun - should be  -32,768 <= value <= 32,767 - value = {}", b16);
		}

		write(b16 >> 8 & 0xFF);
		write(b16 & 0xFF);
	}

	void writeb32(int b32) throws Exception {
		write(b32 >> 24 & 0xFF);
		write(b32 >> 16 & 0xFF);
		write(b32 >> 8 & 0xFF);
		write(b32 & 0xFF);
	}
	
	void writef32(float f32) throws Exception {
    //  int x = Float.floatToIntBits(f32);
    byte[] f = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(f32).array();
    write(f[3] & 0xFF);
    write(f[2] & 0xFF);
    write(f[1] & 0xFF);
    write(f[0] & 0xFF);
	}
	
	void writebu32(long b32) throws Exception {
		write((int)(b32 >> 24 & 0xFF));
		write((int)(b32 >> 16 & 0xFF));
		write((int)(b32 >> 8 & 0xFF));
		write((int)(b32 & 0xFF));
	}

	void write(String str) throws Exception {
		write(str.getBytes());
	}

	void write(int[] array) throws Exception {
		// write size
		write(array.length & 0xFF);

		// write data
		for (int i = 0; i < array.length; ++i) {
			write(array[i] & 0xFF);
		}
	}

	void write(byte[] array) throws Exception {
		// write size
		write(array.length);

		// write data
		for (int i = 0; i < array.length; ++i) {
			write(array[i]);
		}
	}
	
	
	public boolean isRecording() {
		return record != null;
	}
	

	public void record() throws Exception {
		
		if (record == null) {
			record = new FileOutputStream(String.format("%s.ard", arduino.getName()));
		}
	}

	public void stopRecording() {
		if (record != null) {
			try {
				record.close();
			} catch (Exception e) {
			}
			record = null;
		}
	}
	
	public static String deviceTypeToString(int typeId) {
		switch(typeId){
		case 0 :  {
			return "unknown";

		}
		case 1 :  {
			return "Arduino";

		}
		case 2 :  {
			return "UltrasonicSensor";

		}
		case 3 :  {
			return "Stepper";

		}
		case 4 :  {
			return "Motor";

		}
		case 5 :  {
			return "Servo";

		}
		case 6 :  {
			return "Serial";

		}
		case 7 :  {
			return "I2c";

		}
		case 8 :  {
			return "NeoPixel";

		}
		
		default: {
			return "unknown";
		}
		}
	}
  
  /**
   * enable acks on both sides Arduino/Java-Land
   * and MrlComm-land
   */
  public void enableAcks(boolean b){
    // disable local blocking
	  ackEnabled = b;
	  // if (!localOnly){
	  // shutdown MrlComm from sending acks
	  // below is a method only in Msg.java not in VirtualMsg.java
	  // it depends on the definition of enableAck in arduinoMsg.schema  
	  // enableAck(b);
	  // }
	}
	
	public void waitForAck(){
	  if (!ackEnabled || ackRecievedLock.acknowledged){
	    return;
	  }
    synchronized (ackRecievedLock) {
      try {
        long ts = System.currentTimeMillis();
        // log.info("***** starting wait *****");
        ackRecievedLock.wait(2000);
        // log.info("*****  waited {} ms *****", (System.currentTimeMillis() - ts));
      } catch (InterruptedException e) {// don't care}
      }

      if (!ackRecievedLock.acknowledged) {
        //log.error("Ack not received : {} {}", Msg.methodToString(ioCmd[0]), numAck);
        log.error("Ack not received");
      }
    }
	}
	
	public void ackReceived(int function){
	   synchronized (ackRecievedLock) {
	      ackRecievedLock.acknowledged = true;
	      ackRecievedLock.notifyAll();
	    }
	}
	
	public int getMethod(){
	  return method;
	}
	

  public void add(int value) {
    sendBuffer[sendBufferSize] = (value & 0xFF);
    sendBufferSize += 1;
  }
  
  public int[] getBuffer() {    
    return sendBuffer;
  }
	
	public static void main(String[] args) {
		try {

			// FIXME - Test service started or reference retrieved
			// FIXME - subscribe to publishError
			// FIXME - check for any error
			// FIXME - basic design - expected state is connected and ready -
			// between classes it
			// should connect - also dumping serial comm at different levels so
			// virtual arduino in
			// Python can model "real" serial comm
			String port = "COM10";

			LoggingFactory.init(Level.INFO);
			
			/*
			Runtime.start("gui","SwingGui");
			VirtualArduino virtual = (VirtualArduino)Runtime.start("varduino","VirtualArduino");
			virtual.connectVirtualUart(port, port + "UART");
			*/
			
			MrlComm arduino = (MrlComm)Runtime.start("arduino","MrlComm");
			Servo servo01 = (Servo)Runtime.start("servo01","Servo");
			
			/*
			arduino.connect(port);
			
			// test pins
			arduino.enablePin(5);
			
			arduino.disablePin(5);
			
			// test status list enabled
			arduino.enableBoardStatus(true);
			
			servo01.attach(arduino, 8);
			
			servo01.moveTo(30);
			servo01.moveTo(130);
			
			arduino.enableBoardStatus(false);
			*/
			// test ack
			
			// test heartbeat
			
			

		} catch (Exception e) {
			log.error("main threw", e);
		}

	}

}
