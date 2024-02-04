package org.myrobotlab.arduino;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.myrobotlab.service.Serial;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.myrobotlab.logging.Level;

import org.myrobotlab.arduino.virtual.MrlComm;
import org.myrobotlab.string.StringUtil;

/**
 * <pre>
 * 
 Welcome to Msg.java
 Its created by running ArduinoMsgGenerator
 which combines the MrlComm message schema (src/resource/Arduino/arduinoMsg.schema)
 with the cpp template (src/resource/Arduino/generate/Msg.java.template)

   Schema Type Conversions

  Schema      ARDUINO          Java              Range
  none    byte/unsigned char    int (cuz Java byte bites)    1 byte - 0 to 255
  boolean    boolean          boolean              0 1
    b16      int            int (short)            2 bytes  -32,768 to 32,767
    b32      long          int                4 bytes -2,147,483,648 to 2,147,483, 647
    bu32    unsigned long      long              0 to 4,294,967,295
    str      char*, size        String              variable length
    []      byte[], size      int[]              variable length

 All message editing should be done in the arduinoMsg.schema

 The binary wire format of an MrlCommPublisher is:

 MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...
 
 </pre>

 */

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.VirtualArduino;

import java.io.FileOutputStream;
import java.util.Arrays;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * Singlton messaging interface to an MrlCommPublisher
 *
 * @author GroG
 *
 */

public class Msg {

  // TODO: pick a more reasonable timeout.. 3 seconds is high.
  private static final int ACK_TIMEOUT = 3000;
  public transient final static Logger log = LoggerFactory.getLogger(Msg.class);
  public static final int MAX_MSG_SIZE = 64;
  public static final int MAGIC_NUMBER = 170; // 10101010
  public static final int MRLCOMM_VERSION = 68;
  // send buffer
  private int sendBufferSize = 0;
  private int sendBuffer[] = new int[MAX_MSG_SIZE];
  // recv buffer
  private int ioCmd[] = new int[MAX_MSG_SIZE];
  private AtomicInteger byteCount = new AtomicInteger(0);
  private int msgSize = 0;
  // ------ device type mapping constants
  private int method = -1;
  public boolean debug = false;
  // when using a real service, invoke should be true, for unit tests, this should be false.
  private boolean invoke = true;
  
  private int errorServiceToHardwareRxCnt = 0;
  private int errorHardwareToServiceRxCnt = 0;
  
  boolean ackEnabled = true;
  private volatile boolean clearToSend = false;
  public static class AckLock {
    // track if there is a pending message, when sending a message
    // this goes to true. when getting an ack it goes to false.
    volatile boolean pendingMessage = false;
  }
  transient AckLock ackRecievedLock = new AckLock();
  // recording related
  transient OutputStream record = null;
  transient StringBuilder rxBuffer = new StringBuilder();
  transient StringBuilder txBuffer = new StringBuilder();  

  public static final int DEVICE_TYPE_UNKNOWN   =     0;
  public static final int DEVICE_TYPE_ARDUINO   =     1;
  public static final int DEVICE_TYPE_ULTRASONICSENSOR   =     2;
  public static final int DEVICE_TYPE_STEPPER   =     3;
  public static final int DEVICE_TYPE_MOTOR   =     4;
  public static final int DEVICE_TYPE_SERVO   =     5;
  public static final int DEVICE_TYPE_SERIAL   =     6;
  public static final int DEVICE_TYPE_I2C   =     7;
  public static final int DEVICE_TYPE_NEOPIXEL   =     8;
  public static final int DEVICE_TYPE_ENCODER   =     9;
    
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
  // > neoPixelAttach/deviceId/pin/b16 numPixels/depth
  public final static int NEO_PIXEL_ATTACH = 20;
  // > neoPixelSetAnimation/deviceId/animation/red/green/blue/white/b32 wait_ms
  public final static int NEO_PIXEL_SET_ANIMATION = 21;
  // > neoPixelWriteMatrix/deviceId/[] buffer
  public final static int NEO_PIXEL_WRITE_MATRIX = 22;
  // > neoPixelFill/deviceId/b16 address/b16 count/red/green/blue/white
  public final static int NEO_PIXEL_FILL = 23;
  // > neoPixelSetBrightness/deviceId/brightness
  public final static int NEO_PIXEL_SET_BRIGHTNESS = 24;
  // > neoPixelClear/deviceId
  public final static int NEO_PIXEL_CLEAR = 25;
  // > analogWrite/pin/value
  public final static int ANALOG_WRITE = 26;
  // > digitalWrite/pin/value
  public final static int DIGITAL_WRITE = 27;
  // > disablePin/pin
  public final static int DISABLE_PIN = 28;
  // > disablePins
  public final static int DISABLE_PINS = 29;
  // > pinMode/pin/mode
  public final static int PIN_MODE = 30;
  // < publishDebug/str debugMsg
  public final static int PUBLISH_DEBUG = 31;
  // < publishPinArray/[] data
  public final static int PUBLISH_PIN_ARRAY = 32;
  // > setTrigger/pin/triggerValue
  public final static int SET_TRIGGER = 33;
  // > setDebounce/pin/delay
  public final static int SET_DEBOUNCE = 34;
  // > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity/str name
  public final static int SERVO_ATTACH = 35;
  // > servoAttachPin/deviceId/pin
  public final static int SERVO_ATTACH_PIN = 36;
  // > servoDetachPin/deviceId
  public final static int SERVO_DETACH_PIN = 37;
  // > servoSetVelocity/deviceId/b16 velocity
  public final static int SERVO_SET_VELOCITY = 38;
  // > servoSweepStart/deviceId/min/max/step
  public final static int SERVO_SWEEP_START = 39;
  // > servoSweepStop/deviceId
  public final static int SERVO_SWEEP_STOP = 40;
  // > servoMoveToMicroseconds/deviceId/b16 target
  public final static int SERVO_MOVE_TO_MICROSECONDS = 41;
  // > servoSetAcceleration/deviceId/b16 acceleration
  public final static int SERVO_SET_ACCELERATION = 42;
  // < publishServoEvent/deviceId/eventType/b16 currentPos/b16 targetPos
  public final static int PUBLISH_SERVO_EVENT = 43;
  // > serialAttach/deviceId/relayPin
  public final static int SERIAL_ATTACH = 44;
  // > serialRelay/deviceId/[] data
  public final static int SERIAL_RELAY = 45;
  // < publishSerialData/deviceId/[] data
  public final static int PUBLISH_SERIAL_DATA = 46;
  // > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
  public final static int ULTRASONIC_SENSOR_ATTACH = 47;
  // > ultrasonicSensorStartRanging/deviceId
  public final static int ULTRASONIC_SENSOR_START_RANGING = 48;
  // > ultrasonicSensorStopRanging/deviceId
  public final static int ULTRASONIC_SENSOR_STOP_RANGING = 49;
  // < publishUltrasonicSensorData/deviceId/b16 echoTime
  public final static int PUBLISH_ULTRASONIC_SENSOR_DATA = 50;
  // > setAref/b16 type
  public final static int SET_AREF = 51;
  // > motorAttach/deviceId/type/[] pins
  public final static int MOTOR_ATTACH = 52;
  // > motorMove/deviceId/pwr
  public final static int MOTOR_MOVE = 53;
  // > motorMoveTo/deviceId/pos
  public final static int MOTOR_MOVE_TO = 54;
  // > encoderAttach/deviceId/type/pin
  public final static int ENCODER_ATTACH = 55;
  // > setZeroPoint/deviceId
  public final static int SET_ZERO_POINT = 56;
  // < publishEncoderData/deviceId/b16 position
  public final static int PUBLISH_ENCODER_DATA = 57;
  // < publishMrlCommBegin/version
  public final static int PUBLISH_MRL_COMM_BEGIN = 58;
  // > servoStop/deviceId
  public final static int SERVO_STOP = 59;


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
  // public void publishEncoderData(Integer deviceId/*byte*/, Integer position/*b16*/){}
  // public void publishMrlCommBegin(Integer version/*byte*/){}
  
  

  public Msg(MrlCommPublisher arduino, SerialDevice serial) {
    this.arduino = arduino;
    this.serial = serial;
  }
  
  public void begin(SerialDevice serial){
    this.serial = serial;
  }

  // transient private Msg instance;

  // ArduinoSerialCallBacks - TODO - extract interface
  transient private MrlCommPublisher arduino;
  
  transient private SerialDevice serial;
  
  public void processCommand(int[] ioCmd) {
    int startPos = 0;
    method = ioCmd[startPos];
    // always process mrlbegin..
    if (debug) { 
      log.info("Process Command: {} Method: {}", Msg.methodToString(method), ioCmd);
    }
    
    if (method == PUBLISH_ACK) {
      // We saw an ack!  we ack this internally right away, and down below in the generated code, 
      // call publishAck on the MrlCommPublisher
      Integer function = ioCmd[startPos+1]; // bu8
      ackReceived(function);
    }
    
    if (method != PUBLISH_MRL_COMM_BEGIN) {
      if (!clearToSend) {
        log.warn("Not Clear to send yet.  Dumping command {}", ioCmd);
        System.err.println("\nDumping command not clear to send.\n");
        return;
      }
    } else {
      // Process!
      log.info("Clear to process!!!!!!!!!!!!!!!!!!");
      this.clearToSend = true;
    }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
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
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
      }

      break;
    }
    case PUBLISH_ENCODER_DATA: {
      Integer deviceId = ioCmd[startPos+1]; // bu8
      startPos += 1;
      Integer position = b16(ioCmd, startPos+1);
      startPos += 2; //b16
      if(invoke){
        arduino.invoke("publishEncoderData",  deviceId,  position);
      } else { 
         arduino.publishEncoderData( deviceId,  position);
      }
      if(record != null){
        rxBuffer.append("< publishEncoderData");
        rxBuffer.append("/");
        rxBuffer.append(deviceId);
        rxBuffer.append("/");
        rxBuffer.append(position);
        rxBuffer.append("\n");
        try{
          record.write(rxBuffer.toString().getBytes());
          rxBuffer.setLength(0);
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
      }

      break;
    }
    case PUBLISH_MRL_COMM_BEGIN: {
      Integer version = ioCmd[startPos+1]; // bu8
      startPos += 1;
      if(invoke){
        arduino.invoke("publishMrlCommBegin",  version);
      } else { 
         arduino.publishMrlCommBegin( version);
      }
      if(record != null){
        rxBuffer.append("< publishMrlCommBegin");
        rxBuffer.append("/");
        rxBuffer.append(version);
        rxBuffer.append("\n");
        try{
          record.write(rxBuffer.toString().getBytes());
          rxBuffer.setLength(0);
        } catch (IOException e) {
          log.warn("failed recording bytes.", e); 
        }
      }

      break;
    }
    
    }
  }
  

  // Java-land --to--> MrlComm

  public synchronized byte[] getBoardInfo() {
    if (debug) {
      log.info("Sending Message: getBoardInfo to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1); // size
      appendMessage(baos, GET_BOARD_INFO); // msgType = 2
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> getBoardInfo");
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("getBoardInfo threw",e);
      return null;
    }
  }

  public synchronized byte[] enablePin(Integer address/*byte*/, Integer type/*byte*/, Integer rate/*b16*/) {
    if (debug) {
      log.info("Sending Message: enablePin to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 2); // size
      appendMessage(baos, ENABLE_PIN); // msgType = 4
      appendMessage(baos, address);
      appendMessage(baos, type);
      appendMessageb16(baos, rate);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("enablePin threw",e);
      return null;
    }
  }

  public synchronized byte[] setDebug(Boolean enabled/*bool*/) {
    if (debug) {
      log.info("Sending Message: setDebug to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, SET_DEBUG); // msgType = 5
      appendMessagebool(baos, enabled);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> setDebug");
        txBuffer.append("/");
        txBuffer.append(enabled);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("setDebug threw",e);
      return null;
    }
  }

  public synchronized byte[] setSerialRate(Integer rate/*b32*/) {
    if (debug) {
      log.info("Sending Message: setSerialRate to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 4); // size
      appendMessage(baos, SET_SERIAL_RATE); // msgType = 6
      appendMessageb32(baos, rate);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> setSerialRate");
        txBuffer.append("/");
        txBuffer.append(rate);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("setSerialRate threw",e);
      return null;
    }
  }

  public synchronized byte[] softReset() {
    if (debug) {
      log.info("Sending Message: softReset to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1); // size
      appendMessage(baos, SOFT_RESET); // msgType = 7
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> softReset");
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("softReset threw",e);
      return null;
    }
  }

  public synchronized byte[] enableAck(Boolean enabled/*bool*/) {
    if (debug) {
      log.info("Sending Message: enableAck to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, ENABLE_ACK); // msgType = 8
      appendMessagebool(baos, enabled);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> enableAck");
        txBuffer.append("/");
        txBuffer.append(enabled);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("enableAck threw",e);
      return null;
    }
  }

  public synchronized byte[] echo(Float myFloat/*f32*/, Integer myByte/*byte*/, Float secondFloat/*f32*/) {
    if (debug) {
      log.info("Sending Message: echo to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 4 + 1 + 4); // size
      appendMessage(baos, ECHO); // msgType = 10
      appendMessagef32(baos, myFloat);
      appendMessage(baos, myByte);
      appendMessagef32(baos, secondFloat);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("echo threw",e);
      return null;
    }
  }

  public synchronized byte[] customMsg(int[] msg/*[]*/) {
    if (debug) {
      log.info("Sending Message: customMsg to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + (1 + msg.length)); // size
      appendMessage(baos, CUSTOM_MSG); // msgType = 12
      appendMessage(baos, msg);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> customMsg");
        txBuffer.append("/");
        txBuffer.append(Arrays.toString(msg));
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("customMsg threw",e);
      return null;
    }
  }

  public synchronized byte[] deviceDetach(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: deviceDetach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, DEVICE_DETACH); // msgType = 14
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> deviceDetach");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("deviceDetach threw",e);
      return null;
    }
  }

  public synchronized byte[] i2cBusAttach(Integer deviceId/*byte*/, Integer i2cBus/*byte*/) {
    if (debug) {
      log.info("Sending Message: i2cBusAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, I2C_BUS_ATTACH); // msgType = 15
      appendMessage(baos, deviceId);
      appendMessage(baos, i2cBus);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("i2cBusAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] i2cRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer size/*byte*/) {
    if (debug) {
      log.info("Sending Message: i2cRead to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1); // size
      appendMessage(baos, I2C_READ); // msgType = 16
      appendMessage(baos, deviceId);
      appendMessage(baos, deviceAddress);
      appendMessage(baos, size);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("i2cRead threw",e);
      return null;
    }
  }

  public synchronized byte[] i2cWrite(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, int[] data/*[]*/) {
    if (debug) {
      log.info("Sending Message: i2cWrite to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + (1 + data.length)); // size
      appendMessage(baos, I2C_WRITE); // msgType = 17
      appendMessage(baos, deviceId);
      appendMessage(baos, deviceAddress);
      appendMessage(baos, data);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("i2cWrite threw",e);
      return null;
    }
  }

  public synchronized byte[] i2cWriteRead(Integer deviceId/*byte*/, Integer deviceAddress/*byte*/, Integer readSize/*byte*/, Integer writeValue/*byte*/) {
    if (debug) {
      log.info("Sending Message: i2cWriteRead to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1 + 1); // size
      appendMessage(baos, I2C_WRITE_READ); // msgType = 18
      appendMessage(baos, deviceId);
      appendMessage(baos, deviceAddress);
      appendMessage(baos, readSize);
      appendMessage(baos, writeValue);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("i2cWriteRead threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer numPixels/*b16*/, Integer depth/*byte*/) {
    if (debug) {
      log.info("Sending Message: neoPixelAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 2 + 1); // size
      appendMessage(baos, NEO_PIXEL_ATTACH); // msgType = 20
      appendMessage(baos, deviceId);
      appendMessage(baos, pin);
      appendMessageb16(baos, numPixels);
      appendMessage(baos, depth);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> neoPixelAttach");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(pin);
        txBuffer.append("/");
        txBuffer.append(numPixels);
        txBuffer.append("/");
        txBuffer.append(depth);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("neoPixelAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelSetAnimation(Integer deviceId/*byte*/, Integer animation/*byte*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer white/*byte*/, Integer wait_ms/*b32*/) {
    if (debug) {
      log.info("Sending Message: neoPixelSetAnimation to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1 + 1 + 1 + 1 + 4); // size
      appendMessage(baos, NEO_PIXEL_SET_ANIMATION); // msgType = 21
      appendMessage(baos, deviceId);
      appendMessage(baos, animation);
      appendMessage(baos, red);
      appendMessage(baos, green);
      appendMessage(baos, blue);
      appendMessage(baos, white);
      appendMessageb32(baos, wait_ms);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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
        txBuffer.append(white);
        txBuffer.append("/");
        txBuffer.append(wait_ms);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("neoPixelSetAnimation threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelWriteMatrix(Integer deviceId/*byte*/, int[] buffer/*[]*/) {
    if (debug) {
      log.info("Sending Message: neoPixelWriteMatrix to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + (1 + buffer.length)); // size
      appendMessage(baos, NEO_PIXEL_WRITE_MATRIX); // msgType = 22
      appendMessage(baos, deviceId);
      appendMessage(baos, buffer);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("neoPixelWriteMatrix threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelFill(Integer deviceId/*byte*/, Integer address/*b16*/, Integer count/*b16*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer white/*byte*/) {
    if (debug) {
      log.info("Sending Message: neoPixelFill to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 2 + 2 + 1 + 1 + 1 + 1); // size
      appendMessage(baos, NEO_PIXEL_FILL); // msgType = 23
      appendMessage(baos, deviceId);
      appendMessageb16(baos, address);
      appendMessageb16(baos, count);
      appendMessage(baos, red);
      appendMessage(baos, green);
      appendMessage(baos, blue);
      appendMessage(baos, white);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> neoPixelFill");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(address);
        txBuffer.append("/");
        txBuffer.append(count);
        txBuffer.append("/");
        txBuffer.append(red);
        txBuffer.append("/");
        txBuffer.append(green);
        txBuffer.append("/");
        txBuffer.append(blue);
        txBuffer.append("/");
        txBuffer.append(white);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("neoPixelFill threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelSetBrightness(Integer deviceId/*byte*/, Integer brightness/*byte*/) {
    if (debug) {
      log.info("Sending Message: neoPixelSetBrightness to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, NEO_PIXEL_SET_BRIGHTNESS); // msgType = 24
      appendMessage(baos, deviceId);
      appendMessage(baos, brightness);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> neoPixelSetBrightness");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(brightness);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("neoPixelSetBrightness threw",e);
      return null;
    }
  }

  public synchronized byte[] neoPixelClear(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: neoPixelClear to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, NEO_PIXEL_CLEAR); // msgType = 25
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> neoPixelClear");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("neoPixelClear threw",e);
      return null;
    }
  }

  public synchronized byte[] analogWrite(Integer pin/*byte*/, Integer value/*byte*/) {
    if (debug) {
      log.info("Sending Message: analogWrite to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, ANALOG_WRITE); // msgType = 26
      appendMessage(baos, pin);
      appendMessage(baos, value);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("analogWrite threw",e);
      return null;
    }
  }

  public synchronized byte[] digitalWrite(Integer pin/*byte*/, Integer value/*byte*/) {
    if (debug) {
      log.info("Sending Message: digitalWrite to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, DIGITAL_WRITE); // msgType = 27
      appendMessage(baos, pin);
      appendMessage(baos, value);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("digitalWrite threw",e);
      return null;
    }
  }

  public synchronized byte[] disablePin(Integer pin/*byte*/) {
    if (debug) {
      log.info("Sending Message: disablePin to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, DISABLE_PIN); // msgType = 28
      appendMessage(baos, pin);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> disablePin");
        txBuffer.append("/");
        txBuffer.append(pin);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("disablePin threw",e);
      return null;
    }
  }

  public synchronized byte[] disablePins() {
    if (debug) {
      log.info("Sending Message: disablePins to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1); // size
      appendMessage(baos, DISABLE_PINS); // msgType = 29
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> disablePins");
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("disablePins threw",e);
      return null;
    }
  }

  public synchronized byte[] pinMode(Integer pin/*byte*/, Integer mode/*byte*/) {
    if (debug) {
      log.info("Sending Message: pinMode to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, PIN_MODE); // msgType = 30
      appendMessage(baos, pin);
      appendMessage(baos, mode);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("pinMode threw",e);
      return null;
    }
  }

  public synchronized byte[] setTrigger(Integer pin/*byte*/, Integer triggerValue/*byte*/) {
    if (debug) {
      log.info("Sending Message: setTrigger to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, SET_TRIGGER); // msgType = 33
      appendMessage(baos, pin);
      appendMessage(baos, triggerValue);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("setTrigger threw",e);
      return null;
    }
  }

  public synchronized byte[] setDebounce(Integer pin/*byte*/, Integer delay/*byte*/) {
    if (debug) {
      log.info("Sending Message: setDebounce to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, SET_DEBOUNCE); // msgType = 34
      appendMessage(baos, pin);
      appendMessage(baos, delay);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("setDebounce threw",e);
      return null;
    }
  }

  public synchronized byte[] servoAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer initPos/*b16*/, Integer initVelocity/*b16*/, String name/*str*/) {
    if (debug) {
      log.info("Sending Message: servoAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 2 + 2 + (1 + name.length())); // size
      appendMessage(baos, SERVO_ATTACH); // msgType = 35
      appendMessage(baos, deviceId);
      appendMessage(baos, pin);
      appendMessageb16(baos, initPos);
      appendMessageb16(baos, initVelocity);
      appendMessage(baos, name);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] servoAttachPin(Integer deviceId/*byte*/, Integer pin/*byte*/) {
    if (debug) {
      log.info("Sending Message: servoAttachPin to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, SERVO_ATTACH_PIN); // msgType = 36
      appendMessage(baos, deviceId);
      appendMessage(baos, pin);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoAttachPin threw",e);
      return null;
    }
  }

  public synchronized byte[] servoDetachPin(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: servoDetachPin to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, SERVO_DETACH_PIN); // msgType = 37
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> servoDetachPin");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("servoDetachPin threw",e);
      return null;
    }
  }

  public synchronized byte[] servoSetVelocity(Integer deviceId/*byte*/, Integer velocity/*b16*/) {
    if (debug) {
      log.info("Sending Message: servoSetVelocity to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 2); // size
      appendMessage(baos, SERVO_SET_VELOCITY); // msgType = 38
      appendMessage(baos, deviceId);
      appendMessageb16(baos, velocity);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoSetVelocity threw",e);
      return null;
    }
  }

  public synchronized byte[] servoSweepStart(Integer deviceId/*byte*/, Integer min/*byte*/, Integer max/*byte*/, Integer step/*byte*/) {
    if (debug) {
      log.info("Sending Message: servoSweepStart to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1 + 1); // size
      appendMessage(baos, SERVO_SWEEP_START); // msgType = 39
      appendMessage(baos, deviceId);
      appendMessage(baos, min);
      appendMessage(baos, max);
      appendMessage(baos, step);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoSweepStart threw",e);
      return null;
    }
  }

  public synchronized byte[] servoSweepStop(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: servoSweepStop to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, SERVO_SWEEP_STOP); // msgType = 40
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> servoSweepStop");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("servoSweepStop threw",e);
      return null;
    }
  }

  public synchronized byte[] servoMoveToMicroseconds(Integer deviceId/*byte*/, Integer target/*b16*/) {
    if (debug) {
      log.info("Sending Message: servoMoveToMicroseconds to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 2); // size
      appendMessage(baos, SERVO_MOVE_TO_MICROSECONDS); // msgType = 41
      appendMessage(baos, deviceId);
      appendMessageb16(baos, target);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoMoveToMicroseconds threw",e);
      return null;
    }
  }

  public synchronized byte[] servoSetAcceleration(Integer deviceId/*byte*/, Integer acceleration/*b16*/) {
    if (debug) {
      log.info("Sending Message: servoSetAcceleration to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 2); // size
      appendMessage(baos, SERVO_SET_ACCELERATION); // msgType = 42
      appendMessage(baos, deviceId);
      appendMessageb16(baos, acceleration);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("servoSetAcceleration threw",e);
      return null;
    }
  }

  public synchronized byte[] serialAttach(Integer deviceId/*byte*/, Integer relayPin/*byte*/) {
    if (debug) {
      log.info("Sending Message: serialAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, SERIAL_ATTACH); // msgType = 44
      appendMessage(baos, deviceId);
      appendMessage(baos, relayPin);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("serialAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] serialRelay(Integer deviceId/*byte*/, int[] data/*[]*/) {
    if (debug) {
      log.info("Sending Message: serialRelay to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + (1 + data.length)); // size
      appendMessage(baos, SERIAL_RELAY); // msgType = 45
      appendMessage(baos, deviceId);
      appendMessage(baos, data);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("serialRelay threw",e);
      return null;
    }
  }

  public synchronized byte[] ultrasonicSensorAttach(Integer deviceId/*byte*/, Integer triggerPin/*byte*/, Integer echoPin/*byte*/) {
    if (debug) {
      log.info("Sending Message: ultrasonicSensorAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1); // size
      appendMessage(baos, ULTRASONIC_SENSOR_ATTACH); // msgType = 47
      appendMessage(baos, deviceId);
      appendMessage(baos, triggerPin);
      appendMessage(baos, echoPin);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("ultrasonicSensorAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] ultrasonicSensorStartRanging(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: ultrasonicSensorStartRanging to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, ULTRASONIC_SENSOR_START_RANGING); // msgType = 48
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> ultrasonicSensorStartRanging");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("ultrasonicSensorStartRanging threw",e);
      return null;
    }
  }

  public synchronized byte[] ultrasonicSensorStopRanging(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: ultrasonicSensorStopRanging to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, ULTRASONIC_SENSOR_STOP_RANGING); // msgType = 49
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> ultrasonicSensorStopRanging");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("ultrasonicSensorStopRanging threw",e);
      return null;
    }
  }

  public synchronized byte[] setAref(Integer type/*b16*/) {
    if (debug) {
      log.info("Sending Message: setAref to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 2); // size
      appendMessage(baos, SET_AREF); // msgType = 51
      appendMessageb16(baos, type);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> setAref");
        txBuffer.append("/");
        txBuffer.append(type);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("setAref threw",e);
      return null;
    }
  }

  public synchronized byte[] motorAttach(Integer deviceId/*byte*/, Integer type/*byte*/, int[] pins/*[]*/) {
    if (debug) {
      log.info("Sending Message: motorAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + (1 + pins.length)); // size
      appendMessage(baos, MOTOR_ATTACH); // msgType = 52
      appendMessage(baos, deviceId);
      appendMessage(baos, type);
      appendMessage(baos, pins);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("motorAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] motorMove(Integer deviceId/*byte*/, Integer pwr/*byte*/) {
    if (debug) {
      log.info("Sending Message: motorMove to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, MOTOR_MOVE); // msgType = 53
      appendMessage(baos, deviceId);
      appendMessage(baos, pwr);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("motorMove threw",e);
      return null;
    }
  }

  public synchronized byte[] motorMoveTo(Integer deviceId/*byte*/, Integer pos/*byte*/) {
    if (debug) {
      log.info("Sending Message: motorMoveTo to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1); // size
      appendMessage(baos, MOTOR_MOVE_TO); // msgType = 54
      appendMessage(baos, deviceId);
      appendMessage(baos, pos);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
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

      return message;
	} catch (Exception e) {
      log.error("motorMoveTo threw",e);
      return null;
    }
  }

  public synchronized byte[] encoderAttach(Integer deviceId/*byte*/, Integer type/*byte*/, Integer pin/*byte*/) {
    if (debug) {
      log.info("Sending Message: encoderAttach to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1 + 1 + 1); // size
      appendMessage(baos, ENCODER_ATTACH); // msgType = 55
      appendMessage(baos, deviceId);
      appendMessage(baos, type);
      appendMessage(baos, pin);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> encoderAttach");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("/");
        txBuffer.append(type);
        txBuffer.append("/");
        txBuffer.append(pin);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("encoderAttach threw",e);
      return null;
    }
  }

  public synchronized byte[] setZeroPoint(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: setZeroPoint to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, SET_ZERO_POINT); // msgType = 56
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> setZeroPoint");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("setZeroPoint threw",e);
      return null;
    }
  }

  public synchronized byte[] servoStop(Integer deviceId/*byte*/) {
    if (debug) {
      log.info("Sending Message: servoStop to {}", serial.getName());
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      appendMessage(baos, MAGIC_NUMBER);
      appendMessage(baos, 1 + 1); // size
      appendMessage(baos, SERVO_STOP); // msgType = 59
      appendMessage(baos, deviceId);
 
      byte[] message = sendMessage(baos);
      if (ackEnabled){
        waitForAck();
      }
      if(record != null){
        txBuffer.append("> servoStop");
        txBuffer.append("/");
        txBuffer.append(deviceId);
        txBuffer.append("\n");
        record.write(txBuffer.toString().getBytes());
        txBuffer.setLength(0);
      }

      return message;
	} catch (Exception e) {
      log.error("servoStop threw",e);
      return null;
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
    case NEO_PIXEL_FILL:{
      return "neoPixelFill";
    }
    case NEO_PIXEL_SET_BRIGHTNESS:{
      return "neoPixelSetBrightness";
    }
    case NEO_PIXEL_CLEAR:{
      return "neoPixelClear";
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
    case ENCODER_ATTACH:{
      return "encoderAttach";
    }
    case SET_ZERO_POINT:{
      return "setZeroPoint";
    }
    case PUBLISH_ENCODER_DATA:{
      return "publishEncoderData";
    }
    case PUBLISH_MRL_COMM_BEGIN:{
      return "publishMrlCommBegin";
    }
    case SERVO_STOP:{
      return "servoStop";
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
  
  public void onBytes(byte[] bytes) {
    if (debug) {
      // debug message.. semi-human readable?
      String byteString = StringUtil.byteArrayToIntString(bytes);
      log.info("onBytes called byteCount: {} data: >{}<", byteCount, byteString);
    }
    // this gives us the current full buffer that was read from the seral
    for (int i = 0 ; i < bytes.length; i++) {
      // For now, let's just call onByte for each byte upcasted as an int.
      Integer newByte = bytes[i] & 0xFF;
      try {
        byteCount.incrementAndGet();
        if (byteCount.get() == 1) {
          if (newByte != MAGIC_NUMBER) {
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
            // warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++errorServiceToHardwareRxCnt));
            if (!arduino.isConnecting()){
              log.warn("Arduino->MRL error - bad magic number {} - {} rx errors", newByte, ++errorServiceToHardwareRxCnt);
            }
          }
          continue;
        } else if (byteCount.get() == 2) {
          // get the size of message
          if (newByte > 64) {
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            // This is an error scenario.. we should reset our byte count also.
            // error(String.format("Arduino->MRL error %d rx sz errors", ++errorServiceToHardwareRxCnt ));
            log.error("Arduino->MRL error {} rx sz errors", ++errorServiceToHardwareRxCnt);
            continue;
          }
          msgSize = newByte.intValue();
        } else if (byteCount.get() == 3) {
          // This is the method..
          int method = newByte.intValue();
          if (methodToString(method).startsWith("ERROR")) {
            // we've got an error scenario here.. reset the parser and try again!
            log.error("Arduino->MRL error unknown method error. resetting parser.");
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            continue;
          }
          
          // If we're not clear to send, we need to unlock if this is a begin message.
          if (!clearToSend && (method == Msg.PUBLISH_MRL_COMM_BEGIN)) {
            // Clear to send!!
            log.info("Saw the MRL COMM BEGIN!!!!!!!!!!!!! Clear To Send.");
            clearToSend = true;
          } 
          
          if (!clearToSend) {
            if (!arduino.isConnecting()) {
              // we're connecting, so we're going to ignore the message.
              log.warn("NOT CLEAR TO SEND! resetting parser!");
            }
            // We opened the port, and we got some data that isn't a Begin message.
            // so, I think we need to reset the parser and continue processing bytes...
            // there will be errors until the next magic byte is seen.
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            continue;
          }
          // we are in a valid parse state.    
          ioCmd[byteCount.get() - 3] = method;
        } else if (byteCount.get() > 3) {
          // This is the body of the message copy it to the buffer
          ioCmd[byteCount.get() - 3] = newByte.intValue();
        } else {
          // the case where byteCount is negative?! not got.  You should probably never see this.
          log.warn("MRL error rx zero/negative size error: {} {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()));
          //error(String.format("Arduino->MRL error %d rx negsz errors", ++errorServiceToHardwareRxCnt));
          continue;
        }
        // we have a complete message here.
        if (byteCount.get() == 2 + msgSize) {
          // we've received a full message
          int[] actualCommand = Arrays.copyOf(ioCmd, byteCount.get()-2);
          if (debug) {
            log.info("Full message received: {} Data:{}", VirtualMsg.methodToString(ioCmd[0]), actualCommand);
          }
          // process the command.
          processCommand(actualCommand);
          
          // re-init parser
          Arrays.fill(ioCmd, 0); // optimize remove
          msgSize = 0;
          byteCount = new AtomicInteger(0);
        }
      } catch (Exception e) {
        ++errorHardwareToServiceRxCnt ;
        // error("msg structure violation %d", errorHardwareToServiceRxCnt);
        log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()), e);
        // TODO: perhaps we could find the first occurance of 170.. and then attempt to re-parse at that point.
        // find the first occurance of 170 in the bytes subbytes
        // Maybe we can just walk the iterater back to the beginning based on the byte count .. and advance it by 1.. and continue.
        i = i - byteCount.get()+1;
        log.error("Trying to resume parsing the byte stream at position {} bytecount: {}", i, byteCount);
        log.error("Original Byte Array: {}", StringUtil.byteArrayToIntString(bytes));
        System.err.println("Try to consume more messages!");
        msgSize = 0;
        byteCount = new AtomicInteger(0);
        // TODO: this is wonky.. what?! 
        i = 0;
        return;
        
        
      }
    }
    return;
  }

  String F(String msg) {
    return msg;
  }
  
  public void publishError(String error) {
    log.error(error);
  }
  
  void appendMessage(ByteArrayOutputStream baos, int b8) throws Exception {
    if ((b8 < 0) || (b8 > 255)) {
      log.error("writeByte overrun - should be  0 <= value <= 255 - value = {}", b8);
    }
    baos.write(b8 & 0xFF);
  }
  
  void appendMessagebool(ByteArrayOutputStream baos, boolean b1) throws Exception {
    if (b1) {
      appendMessage(baos, 1);
    } else {
      appendMessage(baos, 0);
    }
  }

  void appendMessageb16(ByteArrayOutputStream baos, int b16) throws Exception {
    if ((b16 < -32768) || (b16 > 32767)) {
      log.error("writeByte overrun - should be  -32,768 <= value <= 32,767 - value = {}", b16);
    }
    appendMessage(baos, b16 >> 8 & 0xFF);
    appendMessage(baos, b16 & 0xFF);
  }

  void appendMessageb32(ByteArrayOutputStream baos, int b32) throws Exception {
    appendMessage(baos, b32 >> 24 & 0xFF);
    appendMessage(baos, b32 >> 16 & 0xFF);
    appendMessage(baos, b32 >> 8 & 0xFF);
    appendMessage(baos, b32 & 0xFF);
  }
  
  void appendMessagef32(ByteArrayOutputStream baos, float f32) throws Exception {
    //  int x = Float.floatToIntBits(f32);
    byte[] f = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(f32).array();
    appendMessage(baos, f[3] & 0xFF);
    appendMessage(baos, f[2] & 0xFF);
    appendMessage(baos, f[1] & 0xFF);
    appendMessage(baos, f[0] & 0xFF);
  }
  
  void appendMessagebu32(ByteArrayOutputStream baos, long b32) throws Exception {
    appendMessage(baos, (int)(b32 >> 24 & 0xFF));
    appendMessage(baos, (int)(b32 >> 16 & 0xFF));
    appendMessage(baos, (int)(b32 >> 8 & 0xFF));
    appendMessage(baos, (int)(b32 & 0xFF));
  }

  void appendMessage(ByteArrayOutputStream baos, String str) throws Exception {
    appendMessage(baos, str.getBytes());
  }

  void appendMessage(ByteArrayOutputStream baos, int[] array) throws Exception {
    // write size
    appendMessage(baos, array.length & 0xFF);
    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(baos, array[i] & 0xFF);
    }
  }

  void appendMessage(ByteArrayOutputStream baos, byte[] array) throws Exception {
    // write size
    appendMessage(baos, array.length);
    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(baos, array[i]);
    }
  }
  
  synchronized byte[] sendMessage(ByteArrayOutputStream baos) throws Exception {
    byte[] message = baos.toByteArray();

    if (message.length > MAX_MSG_SIZE) {
      log.error("**** message size {} > MAX_MSG_SIZE {} - not sending ****", MAX_MSG_SIZE, message.length);
      return message;
    }

    if (ackEnabled) {
      // wait for a pending ack to be received before we process our message.^M
      waitForAck();
    }
    // write data if serial not null.
    if (serial != null) {
      // mark it pending before we write the data.
      if (ackEnabled){
        // flip our flag because we're going to send the message now.
        // TODO: is this deadlocked because it's synchronized?!
        // TODO: should this be set regardless of if the serial is null?
        markPending();
      }
      serial.write(message);
      // TODO: if there's an exception, we should clear our pending status?
      if (ackEnabled) {
        // wait for a pending ack to be received before we process our message.^M
        waitForAck();
      }
    }
    return message;
  }
  
  public void markPending() {
    if (debug) {
      log.info("Setting pending flag.");
    }
    synchronized (ackRecievedLock) {
      ackRecievedLock.pendingMessage = true;
      ackRecievedLock.notifyAll();
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
        log.info("Error closing recording stream. ", e);
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
    case 9 :  {
      return "Encoder";

    }
    
    default: {
      return "unknown";
    }
    }
  }
  
  public void enableAcks(boolean b){
    ackEnabled = b;
    // if (!localOnly){
    // shutdown MrlComm from sending acks
    // below is a method only in Msg.java not in VirtualMsg.java
    // it depends on the definition of enableAck in arduinoMsg.schema  
    // enableAck(b);
    // }
  }
  
  public void waitForAck(){
    if (!ackEnabled || serial == null || !serial.isConnected()) {
      return;
    }
    // if there's a pending message, we need to wait for the ack to be received.
    if (ackRecievedLock.pendingMessage) {
      synchronized (ackRecievedLock) {
        try {
          ackRecievedLock.wait(ACK_TIMEOUT);
        } catch (InterruptedException e) {
        }
        if (ackRecievedLock.pendingMessage) {
          log.error("Ack not received, ack timeout!");
          // TODO: should we just reset and hope for the best? maybe trigger a sync?
          // ackRecievedLock.pendingMessage = false;
          arduino.ackTimeout();
        }
      }
    }
  }
  
  public void ackReceived(int function) {
    synchronized (ackRecievedLock) {
      ackRecievedLock.pendingMessage = false;
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
      MrlCommPublisher arduino = (MrlCommPublisher)Runtime.start("arduino","MrlCommPublisher");
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

  public void onConnect(String portName) {
    if (debug) {
      log.info("On Connect Called in Msg.");
    }
    // reset the parser...
    this.byteCount = new AtomicInteger(0);
    this.msgSize = 0;
    ackReceived(-1);
  }

  public void onDisconnect(String portName) {
    if (debug) {
      log.info("On Disconnect Called in Msg.");
    }
    // reset the parser... this might not be necessary.
    this.byteCount = new AtomicInteger(0);
    this.msgSize = 0;
    ackReceived(-1);
  }

  public static boolean isFullMessage(byte[] bytes) {
    // Criteria that a sequence of bytes could be parsed as a complete message.
    // can't be null
    if (bytes == null) 
      return false;
    // it's got to be at least 3 bytes long.  magic + method + size
    if (bytes.length <= 2) 
      return false;
    // first byte has to be magic
    if ((bytes[0] & 0xFF) != Msg.MAGIC_NUMBER) 
      return false;
    
    int method = bytes[1] & 0xFF;
    String strMethod = Msg.methodToString(method); 
    // only known methods. 
    // TODO: make the methodToString return null for an unknown lookup.
    if (strMethod.startsWith("ERROR")) 
      return false;
    
    // now it's got to be the proper length
    int length = bytes[1] & 0xFF;
    // max message size is 64 bytes
    if (length > 64)
      return false;

    // it's a exactly a full message or a message and more.
    if (bytes.length >= length+2)
      return true;

    
    return false;
  }

  public boolean isClearToSend() {
    return clearToSend;
  }
  
  public void setInvoke(boolean b){	
    invoke = b;	
  }

  public void setSerial(Serial serial) {
    this.serial = serial;
  }

}
