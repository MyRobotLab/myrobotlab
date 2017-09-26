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

 The binary wire format of an Arduino is:

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
 * Singlton messaging interface to an Arduino
 *
 * @author GroG
 *
 */

public class Msg {

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
	
	boolean ackEnabled = true;
	
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
	
	// public void publishMRLCommError(String errorMsg/*str*/){}
	// public void publishBoardInfo(Integer version/*byte*/, Integer boardType/*byte*/, Integer microsPerLoop/*b16*/, Integer sram/*b16*/, Integer activePins/*byte*/, int[] deviceSummary/*[]*/){}
	// public void publishAck(Integer function/*byte*/){}
	// public void publishEcho(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/){}
	// public void publishCustomMsg(int[] msg/*[]*/){}
	// public void publishI2cData(Integer deviceId/*byte*/, int[] data/*[]*/){}
	// public void publishDebug(String debugMsg/*str*/){}
	// public void publishPinArray(int[] data/*[]*/){}
	// public void publishServoEvent(Integer deviceId/*byte*/, Integer eventType/*byte*/, Integer currentPos/*b16*/, Integer targetPos/*b16*/){}
	// public void publishSerialData(Integer deviceId/*byte*/, int[] data/*[]*/){}
	// public void publishUltrasonicSensorData(Integer deviceId/*byte*/, Integer echoTime/*b16*/){}
	

	
	public transient final static Logger log = LoggerFactory.getLogger(Msg.class);

	public Msg(Arduino arduino, SerialDevice serial) {
		this.arduino = arduino;
		this.serial = serial;
	}
	
	public void begin(SerialDevice serial){
	  this.serial = serial;
	}

	// transient private Msg instance;

	// ArduinoSerialCallBacks - TODO - extract interface
	transient private Arduino arduino;
	
	transient private SerialDevice serial;

	/**
	 * want to grab it when SerialDevice is created
	 *
	 * @param serial
	 * @return
	 */
	/*
	static public synchronized Msg getInstance(Arduino arduino, SerialDevice serial) {
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
		case PUBLISH_MRLCOMM_ERROR: {
			String errorMsg = str(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishMRLCommError",  errorMsg);
			} else { 
 				arduino.publishMRLCommError( errorMsg);
			}
			if(record != null){
				rxBuffer.append("< publishMRLCommError");
				rxBuffer.append("/");
				rxBuffer.append(errorMsg);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_BOARD_INFO: {
			Integer version = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer boardType = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer microsPerLoop = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			Integer sram = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			Integer activePins = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] deviceSummary = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishBoardInfo",  version,  boardType,  microsPerLoop,  sram,  activePins,  deviceSummary);
			} else { 
 				arduino.publishBoardInfo( version,  boardType,  microsPerLoop,  sram,  activePins,  deviceSummary);
			}
			if(record != null){
				rxBuffer.append("< publishBoardInfo");
				rxBuffer.append("/");
				rxBuffer.append(version);
				rxBuffer.append("/");
				rxBuffer.append(boardType);
				rxBuffer.append("/");
				rxBuffer.append(microsPerLoop);
				rxBuffer.append("/");
				rxBuffer.append(sram);
				rxBuffer.append("/");
				rxBuffer.append(activePins);
				rxBuffer.append("/");
				rxBuffer.append(Arrays.toString(deviceSummary));
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_ACK: {
			Integer function = ioCmd[startPos+1]; // bu8
			startPos += 1;
			if(invoke){
				arduino.invoke("publishAck",  function);
			} else { 
 				arduino.publishAck( function);
			}
			if(record != null){
				rxBuffer.append("< publishAck");
				rxBuffer.append("/");
				rxBuffer.append(function);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_ECHO: {
			Float myFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			Integer myByte = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Float secondFloat = f32(ioCmd, startPos+1);
			startPos += 4; //f32
			if(invoke){
				arduino.invoke("publishEcho",  myFloat,  myByte,  secondFloat);
			} else { 
 				arduino.publishEcho( myFloat,  myByte,  secondFloat);
			}
			if(record != null){
				rxBuffer.append("< publishEcho");
				rxBuffer.append("/");
				rxBuffer.append(myFloat);
				rxBuffer.append("/");
				rxBuffer.append(myByte);
				rxBuffer.append("/");
				rxBuffer.append(secondFloat);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_CUSTOM_MSG: {
			int[] msg = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishCustomMsg",  msg);
			} else { 
 				arduino.publishCustomMsg( msg);
			}
			if(record != null){
				rxBuffer.append("< publishCustomMsg");
				rxBuffer.append("/");
				rxBuffer.append(Arrays.toString(msg));
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_I2C_DATA: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishI2cData",  deviceId,  data);
			} else { 
 				arduino.publishI2cData( deviceId,  data);
			}
			if(record != null){
				rxBuffer.append("< publishI2cData");
				rxBuffer.append("/");
				rxBuffer.append(deviceId);
				rxBuffer.append("/");
				rxBuffer.append(Arrays.toString(data));
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_DEBUG: {
			String debugMsg = str(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishDebug",  debugMsg);
			} else { 
 				arduino.publishDebug( debugMsg);
			}
			if(record != null){
				rxBuffer.append("< publishDebug");
				rxBuffer.append("/");
				rxBuffer.append(debugMsg);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_PIN_ARRAY: {
			int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishPinArray",  data);
			} else { 
 				arduino.publishPinArray( data);
			}
			if(record != null){
				rxBuffer.append("< publishPinArray");
				rxBuffer.append("/");
				rxBuffer.append(Arrays.toString(data));
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_SERVO_EVENT: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer eventType = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer currentPos = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			Integer targetPos = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("publishServoEvent",  deviceId,  eventType,  currentPos,  targetPos);
			} else { 
 				arduino.publishServoEvent( deviceId,  eventType,  currentPos,  targetPos);
			}
			if(record != null){
				rxBuffer.append("< publishServoEvent");
				rxBuffer.append("/");
				rxBuffer.append(deviceId);
				rxBuffer.append("/");
				rxBuffer.append(eventType);
				rxBuffer.append("/");
				rxBuffer.append(currentPos);
				rxBuffer.append("/");
				rxBuffer.append(targetPos);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_SERIAL_DATA: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			int[] data = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);
			startPos += 1 + ioCmd[startPos+1];
			if(invoke){
				arduino.invoke("publishSerialData",  deviceId,  data);
			} else { 
 				arduino.publishSerialData( deviceId,  data);
			}
			if(record != null){
				rxBuffer.append("< publishSerialData");
				rxBuffer.append("/");
				rxBuffer.append(deviceId);
				rxBuffer.append("/");
				rxBuffer.append(Arrays.toString(data));
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		case PUBLISH_ULTRASONIC_SENSOR_DATA: {
			Integer deviceId = ioCmd[startPos+1]; // bu8
			startPos += 1;
			Integer echoTime = b16(ioCmd, startPos+1);
			startPos += 2; //b16
			if(invoke){
				arduino.invoke("publishUltrasonicSensorData",  deviceId,  echoTime);
			} else { 
 				arduino.publishUltrasonicSensorData( deviceId,  echoTime);
			}
			if(record != null){
				rxBuffer.append("< publishUltrasonicSensorData");
				rxBuffer.append("/");
				rxBuffer.append(deviceId);
				rxBuffer.append("/");
				rxBuffer.append(echoTime);
			rxBuffer.append("\n");
			try{
				record.write(rxBuffer.toString().getBytes());
				rxBuffer.setLength(0);
			}catch(IOException e){}
			}

			break;
		}
		
		}
	}
	

	// Java-land --to--> MrlComm

	public synchronized void getBoardInfo() {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1); // size
			write(GET_BOARD_INFO); // msgType = 2
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> getBoardInfo");
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("getBoardInfo threw",e);
	  }
	}

	public synchronized void enablePin(Integer address/*byte*/, Integer type/*byte*/, Integer rate/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 2); // size
			write(ENABLE_PIN); // msgType = 4
			write(address);
			write(type);
			writeb16(rate);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> enablePin");
				txBuffer.append("/");
				txBuffer.append(address);
				txBuffer.append("/");
				txBuffer.append(type);
				txBuffer.append("/");
				txBuffer.append(rate);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("enablePin threw",e);
	  }
	}

	public synchronized void setDebug(Boolean enabled/*bool*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(SET_DEBUG); // msgType = 5
			writebool(enabled);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> setDebug");
				txBuffer.append("/");
				txBuffer.append(enabled);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("setDebug threw",e);
	  }
	}

	public synchronized void setSerialRate(Integer rate/*b32*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 4); // size
			write(SET_SERIAL_RATE); // msgType = 6
			writeb32(rate);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> setSerialRate");
				txBuffer.append("/");
				txBuffer.append(rate);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("setSerialRate threw",e);
	  }
	}

	public synchronized void softReset() {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1); // size
			write(SOFT_RESET); // msgType = 7
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> softReset");
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("softReset threw",e);
	  }
	}

	public synchronized void enableAck(Boolean enabled/*bool*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(ENABLE_ACK); // msgType = 8
			writebool(enabled);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> enableAck");
				txBuffer.append("/");
				txBuffer.append(enabled);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("enableAck threw",e);
	  }
	}

	public synchronized void echo(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 4 + 1 + 4); // size
			write(ECHO); // msgType = 10
			writef32(myFloat);
			write(myByte);
			writef32(secondFloat);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> echo");
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
	  			log.error("echo threw",e);
	  }
	}

	public synchronized void customMsg(int[] msg/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + (1 + msg.length)); // size
			write(CUSTOM_MSG); // msgType = 12
			write(msg);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> customMsg");
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(msg));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("customMsg threw",e);
	  }
	}

	public synchronized void deviceDetach(Integer deviceId/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(DEVICE_DETACH); // msgType = 14
			write(deviceId);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> deviceDetach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("deviceDetach threw",e);
	  }
	}

	public synchronized void i2cBusAttach(Integer deviceId/*byte*/, Integer i2cBus/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(I2C_BUS_ATTACH); // msgType = 15
			write(deviceId);
			write(i2cBus);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> i2cBusAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(i2cBus);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("i2cBusAttach threw",e);
	  }
	}

	public synchronized void i2cRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer size/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 1); // size
			write(I2C_READ); // msgType = 16
			write(deviceId);
			write(deviceAddress);
			write(size);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> i2cRead");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(deviceAddress);
				txBuffer.append("/");
				txBuffer.append(size);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("i2cRead threw",e);
	  }
	}

	public synchronized void i2cWrite(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, int[] data/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + (1 + data.length)); // size
			write(I2C_WRITE); // msgType = 17
			write(deviceId);
			write(deviceAddress);
			write(data);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> i2cWrite");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(deviceAddress);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(data));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("i2cWrite threw",e);
	  }
	}

	public synchronized void i2cWriteRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer readSize/*byte*/, Integer writeValue/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 1 + 1); // size
			write(I2C_WRITE_READ); // msgType = 18
			write(deviceId);
			write(deviceAddress);
			write(readSize);
			write(writeValue);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> i2cWriteRead");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(deviceAddress);
				txBuffer.append("/");
				txBuffer.append(readSize);
				txBuffer.append("/");
				txBuffer.append(writeValue);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("i2cWriteRead threw",e);
	  }
	}

	public synchronized void neoPixelAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer numPixels/*b32*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 4); // size
			write(NEO_PIXEL_ATTACH); // msgType = 20
			write(deviceId);
			write(pin);
			writeb32(numPixels);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> neoPixelAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(numPixels);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("neoPixelAttach threw",e);
	  }
	}

	public synchronized void neoPixelSetAnimation(Integer deviceId/*byte*/, Integer animation/*byte*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer speed/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 1 + 1 + 1 + 2); // size
			write(NEO_PIXEL_SET_ANIMATION); // msgType = 21
			write(deviceId);
			write(animation);
			write(red);
			write(green);
			write(blue);
			writeb16(speed);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> neoPixelSetAnimation");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(animation);
				txBuffer.append("/");
				txBuffer.append(red);
				txBuffer.append("/");
				txBuffer.append(green);
				txBuffer.append("/");
				txBuffer.append(blue);
				txBuffer.append("/");
				txBuffer.append(speed);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("neoPixelSetAnimation threw",e);
	  }
	}

	public synchronized void neoPixelWriteMatrix(Integer deviceId/*byte*/, int[] buffer/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + (1 + buffer.length)); // size
			write(NEO_PIXEL_WRITE_MATRIX); // msgType = 22
			write(deviceId);
			write(buffer);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> neoPixelWriteMatrix");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(buffer));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("neoPixelWriteMatrix threw",e);
	  }
	}

	public synchronized void analogWrite(Integer pin/*byte*/, Integer value/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(ANALOG_WRITE); // msgType = 23
			write(pin);
			write(value);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> analogWrite");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(value);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("analogWrite threw",e);
	  }
	}

	public synchronized void digitalWrite(Integer pin/*byte*/, Integer value/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(DIGITAL_WRITE); // msgType = 24
			write(pin);
			write(value);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> digitalWrite");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(value);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("digitalWrite threw",e);
	  }
	}

	public synchronized void disablePin(Integer pin/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(DISABLE_PIN); // msgType = 25
			write(pin);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> disablePin");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("disablePin threw",e);
	  }
	}

	public synchronized void disablePins() {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1); // size
			write(DISABLE_PINS); // msgType = 26
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> disablePins");
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("disablePins threw",e);
	  }
	}

	public synchronized void pinMode(Integer pin/*byte*/, Integer mode/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(PIN_MODE); // msgType = 27
			write(pin);
			write(mode);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> pinMode");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(mode);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("pinMode threw",e);
	  }
	}

	public synchronized void setTrigger(Integer pin/*byte*/, Integer triggerValue/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(SET_TRIGGER); // msgType = 30
			write(pin);
			write(triggerValue);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> setTrigger");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(triggerValue);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("setTrigger threw",e);
	  }
	}

	public synchronized void setDebounce(Integer pin/*byte*/, Integer delay/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(SET_DEBOUNCE); // msgType = 31
			write(pin);
			write(delay);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> setDebounce");
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(delay);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("setDebounce threw",e);
	  }
	}

	public synchronized void servoAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer initPos/*b16*/, Integer initVelocity/*b16*/, String name/*str*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 2 + 2 + (1 + name.length())); // size
			write(SERVO_ATTACH); // msgType = 32
			write(deviceId);
			write(pin);
			writeb16(initPos);
			writeb16(initVelocity);
			write(name);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("/");
				txBuffer.append(initPos);
				txBuffer.append("/");
				txBuffer.append(initVelocity);
				txBuffer.append("/");
				txBuffer.append(name);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoAttach threw",e);
	  }
	}

	public synchronized void servoAttachPin(Integer deviceId/*byte*/, Integer pin/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(SERVO_ATTACH_PIN); // msgType = 33
			write(deviceId);
			write(pin);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoAttachPin");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(pin);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoAttachPin threw",e);
	  }
	}

	public synchronized void servoDetachPin(Integer deviceId/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(SERVO_DETACH_PIN); // msgType = 34
			write(deviceId);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoDetachPin");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoDetachPin threw",e);
	  }
	}

	public synchronized void servoSetVelocity(Integer deviceId/*byte*/, Integer velocity/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 2); // size
			write(SERVO_SET_VELOCITY); // msgType = 35
			write(deviceId);
			writeb16(velocity);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoSetVelocity");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(velocity);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoSetVelocity threw",e);
	  }
	}

	public synchronized void servoSweepStart(Integer deviceId/*byte*/, Integer min/*byte*/, Integer max/*byte*/, Integer step/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 1 + 1); // size
			write(SERVO_SWEEP_START); // msgType = 36
			write(deviceId);
			write(min);
			write(max);
			write(step);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoSweepStart");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(min);
				txBuffer.append("/");
				txBuffer.append(max);
				txBuffer.append("/");
				txBuffer.append(step);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoSweepStart threw",e);
	  }
	}

	public synchronized void servoSweepStop(Integer deviceId/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(SERVO_SWEEP_STOP); // msgType = 37
			write(deviceId);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoSweepStop");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoSweepStop threw",e);
	  }
	}

	public synchronized void servoMoveToMicroseconds(Integer deviceId/*byte*/, Integer target/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 2); // size
			write(SERVO_MOVE_TO_MICROSECONDS); // msgType = 38
			write(deviceId);
			writeb16(target);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoMoveToMicroseconds");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(target);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoMoveToMicroseconds threw",e);
	  }
	}

	public synchronized void servoSetAcceleration(Integer deviceId/*byte*/, Integer acceleration/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 2); // size
			write(SERVO_SET_ACCELERATION); // msgType = 39
			write(deviceId);
			writeb16(acceleration);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> servoSetAcceleration");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(acceleration);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("servoSetAcceleration threw",e);
	  }
	}

	public synchronized void serialAttach(Integer deviceId/*byte*/, Integer relayPin/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(SERIAL_ATTACH); // msgType = 41
			write(deviceId);
			write(relayPin);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> serialAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(relayPin);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("serialAttach threw",e);
	  }
	}

	public synchronized void serialRelay(Integer deviceId/*byte*/, int[] data/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + (1 + data.length)); // size
			write(SERIAL_RELAY); // msgType = 42
			write(deviceId);
			write(data);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> serialRelay");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(data));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("serialRelay threw",e);
	  }
	}

	public synchronized void ultrasonicSensorAttach(Integer deviceId/*byte*/, Integer triggerPin/*byte*/, Integer echoPin/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + 1); // size
			write(ULTRASONIC_SENSOR_ATTACH); // msgType = 44
			write(deviceId);
			write(triggerPin);
			write(echoPin);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> ultrasonicSensorAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(triggerPin);
				txBuffer.append("/");
				txBuffer.append(echoPin);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("ultrasonicSensorAttach threw",e);
	  }
	}

	public synchronized void ultrasonicSensorStartRanging(Integer deviceId/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(ULTRASONIC_SENSOR_START_RANGING); // msgType = 45
			write(deviceId);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> ultrasonicSensorStartRanging");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("ultrasonicSensorStartRanging threw",e);
	  }
	}

	public synchronized void ultrasonicSensorStopRanging(Integer deviceId/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1); // size
			write(ULTRASONIC_SENSOR_STOP_RANGING); // msgType = 46
			write(deviceId);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> ultrasonicSensorStopRanging");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("ultrasonicSensorStopRanging threw",e);
	  }
	}

	public synchronized void setAref(Integer type/*b16*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 2); // size
			write(SET_AREF); // msgType = 48
			writeb16(type);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> setAref");
				txBuffer.append("/");
				txBuffer.append(type);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("setAref threw",e);
	  }
	}

	public synchronized void motorAttach(Integer deviceId/*byte*/, Integer type/*byte*/, int[] pins/*[]*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1 + (1 + pins.length)); // size
			write(MOTOR_ATTACH); // msgType = 49
			write(deviceId);
			write(type);
			write(pins);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> motorAttach");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(type);
				txBuffer.append("/");
				txBuffer.append(Arrays.toString(pins));
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("motorAttach threw",e);
	  }
	}

	public synchronized void motorMove(Integer deviceId/*byte*/, Integer pwr/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(MOTOR_MOVE); // msgType = 50
			write(deviceId);
			write(pwr);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> motorMove");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(pwr);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("motorMove threw",e);
	  }
	}

	public synchronized void motorMoveTo(Integer deviceId/*byte*/, Integer pos/*byte*/) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(1 + 1 + 1); // size
			write(MOTOR_MOVE_TO); // msgType = 51
			write(deviceId);
			write(pos);
 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
			if(record != null){
				txBuffer.append("> motorMoveTo");
				txBuffer.append("/");
				txBuffer.append(deviceId);
				txBuffer.append("/");
				txBuffer.append(pos);
				txBuffer.append("\n");
				record.write(txBuffer.toString().getBytes());
				txBuffer.setLength(0);
			}

	  } catch (Exception e) {
	  			log.error("motorMoveTo threw",e);
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
			
			Arduino arduino = (Arduino)Runtime.start("arduino","Arduino");
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
