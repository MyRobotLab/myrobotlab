package org.myrobotlab.arduino;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

 The binary wire format of an MrlCommListener is:

 MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...
 
 </pre>

 */

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.VirtualArduino;

import java.io.FileOutputStream;
import java.util.Arrays;
import org.myrobotlab.service.interfaces.MrlCommListener;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * Singlton messaging interface to an MrlCommListener
 *
 * @author GroG
 *
 */

public class Msg {

  public static final int MAX_MSG_SIZE = 64;
  public static final int MAGIC_NUMBER = 170; // 10101010
  public static final int MRLCOMM_VERSION = 64;
  
  int ackMaxWaitMs = 1000;
  
    boolean waiting = false;
  
  
  // send buffer
  int sendBufferSize = 0;
  int sendBuffer[] = new int[MAX_MSG_SIZE];
  
  // recv buffer
  int ioCmd[] = new int[MAX_MSG_SIZE];
  
  AtomicInteger byteCount = new AtomicInteger(0);
  int msgSize = 0;

  // ------ device type mapping constants
  int method = -1;
  public boolean debug = false;
  boolean invoke = true;
  
  private int errorServiceToHardwareRxCnt = 0;
  private int errorHardwareToServiceRxCnt = 0;
  
  boolean ackEnabled = true;
  private ByteArrayOutputStream baos = null;
  public volatile boolean pendingMessage = false;
  public volatile boolean clearToSend = false;
    
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
  // > encoderAttach/deviceId/type/pin
  public final static int ENCODER_ATTACH = 52;
  // > setZeroPoint/deviceId
  public final static int SET_ZERO_POINT = 53;
  // < publishEncoderData/deviceId/b16 position
  public final static int PUBLISH_ENCODER_DATA = 54;
  // < publishMrlCommBegin/version
  public final static int PUBLISH_MRL_COMM_BEGIN = 55;
  // > servoStop/deviceId
  public final static int SERVO_STOP = 56;


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
  

  
  public transient final static Logger log = LoggerFactory.getLogger(Msg.class);

  public Msg(MrlCommListener arduino, SerialDevice serial) {
    this.arduino = arduino;
    this.serial = serial;
  }
  
  public void begin(SerialDevice serial){
    this.serial = serial;
  }

  // transient private Msg instance;

  // ArduinoSerialCallBacks - TODO - extract interface
  transient private MrlCommListener arduino;
  
  transient private SerialDevice serial;
  
  public void setInvoke(boolean b){
    invoke = b;
  }
  
  public void processCommand(){
    processCommand(ioCmd);
  }
  
  public void processCommand(int[] ioCmd) {
    int startPos = 0;
    method = ioCmd[startPos];
    // always process mrlbegin.. 
    log.info("Process Command: {} Method: {}", method, ioCmd);
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
      }catch(IOException e){}
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
      }catch(IOException e){}
      }

      break;
    }
    
    }
  }
  

  // Java-land --to--> MrlComm

  public synchronized byte[] getBoardInfo() {
    log.info("Sending Messge: getBoardInfo");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1); // size
      appendMessage(GET_BOARD_INFO); // msgType = 2
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: enablePin");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 2); // size
      appendMessage(ENABLE_PIN); // msgType = 4
      appendMessage(address);
      appendMessage(type);
      appendMessageb16(rate);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setDebug");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(SET_DEBUG); // msgType = 5
      appendMessagebool(enabled);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setSerialRate");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 4); // size
      appendMessage(SET_SERIAL_RATE); // msgType = 6
      appendMessageb32(rate);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: softReset");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1); // size
      appendMessage(SOFT_RESET); // msgType = 7
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: enableAck");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(ENABLE_ACK); // msgType = 8
      appendMessagebool(enabled);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: echo");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 4 + 1 + 4); // size
      appendMessage(ECHO); // msgType = 10
      appendMessagef32(myFloat);
      appendMessage(myByte);
      appendMessagef32(secondFloat);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: customMsg");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + (1 + msg.length)); // size
      appendMessage(CUSTOM_MSG); // msgType = 12
      appendMessage(msg);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: deviceDetach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(DEVICE_DETACH); // msgType = 14
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: i2cBusAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(I2C_BUS_ATTACH); // msgType = 15
      appendMessage(deviceId);
      appendMessage(i2cBus);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: i2cRead");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1); // size
      appendMessage(I2C_READ); // msgType = 16
      appendMessage(deviceId);
      appendMessage(deviceAddress);
      appendMessage(size);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: i2cWrite");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + (1 + data.length)); // size
      appendMessage(I2C_WRITE); // msgType = 17
      appendMessage(deviceId);
      appendMessage(deviceAddress);
      appendMessage(data);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: i2cWriteRead");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1 + 1); // size
      appendMessage(I2C_WRITE_READ); // msgType = 18
      appendMessage(deviceId);
      appendMessage(deviceAddress);
      appendMessage(readSize);
      appendMessage(writeValue);
 
      byte[] message = sendMessage();
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

  public synchronized byte[] neoPixelAttach(Integer deviceId/*byte*/, Integer pin/*byte*/, Integer numPixels/*b32*/) {
    log.info("Sending Messge: neoPixelAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 4); // size
      appendMessage(NEO_PIXEL_ATTACH); // msgType = 20
      appendMessage(deviceId);
      appendMessage(pin);
      appendMessageb32(numPixels);
 
      byte[] message = sendMessage();
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

  public synchronized byte[] neoPixelSetAnimation(Integer deviceId/*byte*/, Integer animation/*byte*/, Integer red/*byte*/, Integer green/*byte*/, Integer blue/*byte*/, Integer speed/*b16*/) {
    log.info("Sending Messge: neoPixelSetAnimation");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1 + 1 + 1 + 2); // size
      appendMessage(NEO_PIXEL_SET_ANIMATION); // msgType = 21
      appendMessage(deviceId);
      appendMessage(animation);
      appendMessage(red);
      appendMessage(green);
      appendMessage(blue);
      appendMessageb16(speed);
 
      byte[] message = sendMessage();
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
        txBuffer.append(speed);
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
    log.info("Sending Messge: neoPixelWriteMatrix");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + (1 + buffer.length)); // size
      appendMessage(NEO_PIXEL_WRITE_MATRIX); // msgType = 22
      appendMessage(deviceId);
      appendMessage(buffer);
 
      byte[] message = sendMessage();
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

  public synchronized byte[] analogWrite(Integer pin/*byte*/, Integer value/*byte*/) {
    log.info("Sending Messge: analogWrite");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(ANALOG_WRITE); // msgType = 23
      appendMessage(pin);
      appendMessage(value);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: digitalWrite");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(DIGITAL_WRITE); // msgType = 24
      appendMessage(pin);
      appendMessage(value);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: disablePin");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(DISABLE_PIN); // msgType = 25
      appendMessage(pin);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: disablePins");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1); // size
      appendMessage(DISABLE_PINS); // msgType = 26
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: pinMode");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(PIN_MODE); // msgType = 27
      appendMessage(pin);
      appendMessage(mode);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setTrigger");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(SET_TRIGGER); // msgType = 30
      appendMessage(pin);
      appendMessage(triggerValue);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setDebounce");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(SET_DEBOUNCE); // msgType = 31
      appendMessage(pin);
      appendMessage(delay);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 2 + 2 + (1 + name.length())); // size
      appendMessage(SERVO_ATTACH); // msgType = 32
      appendMessage(deviceId);
      appendMessage(pin);
      appendMessageb16(initPos);
      appendMessageb16(initVelocity);
      appendMessage(name);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoAttachPin");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(SERVO_ATTACH_PIN); // msgType = 33
      appendMessage(deviceId);
      appendMessage(pin);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoDetachPin");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(SERVO_DETACH_PIN); // msgType = 34
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoSetVelocity");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 2); // size
      appendMessage(SERVO_SET_VELOCITY); // msgType = 35
      appendMessage(deviceId);
      appendMessageb16(velocity);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoSweepStart");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1 + 1); // size
      appendMessage(SERVO_SWEEP_START); // msgType = 36
      appendMessage(deviceId);
      appendMessage(min);
      appendMessage(max);
      appendMessage(step);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoSweepStop");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(SERVO_SWEEP_STOP); // msgType = 37
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoMoveToMicroseconds");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 2); // size
      appendMessage(SERVO_MOVE_TO_MICROSECONDS); // msgType = 38
      appendMessage(deviceId);
      appendMessageb16(target);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoSetAcceleration");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 2); // size
      appendMessage(SERVO_SET_ACCELERATION); // msgType = 39
      appendMessage(deviceId);
      appendMessageb16(acceleration);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: serialAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(SERIAL_ATTACH); // msgType = 41
      appendMessage(deviceId);
      appendMessage(relayPin);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: serialRelay");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + (1 + data.length)); // size
      appendMessage(SERIAL_RELAY); // msgType = 42
      appendMessage(deviceId);
      appendMessage(data);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: ultrasonicSensorAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1); // size
      appendMessage(ULTRASONIC_SENSOR_ATTACH); // msgType = 44
      appendMessage(deviceId);
      appendMessage(triggerPin);
      appendMessage(echoPin);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: ultrasonicSensorStartRanging");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(ULTRASONIC_SENSOR_START_RANGING); // msgType = 45
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: ultrasonicSensorStopRanging");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(ULTRASONIC_SENSOR_STOP_RANGING); // msgType = 46
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setAref");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 2); // size
      appendMessage(SET_AREF); // msgType = 48
      appendMessageb16(type);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: motorAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + (1 + pins.length)); // size
      appendMessage(MOTOR_ATTACH); // msgType = 49
      appendMessage(deviceId);
      appendMessage(type);
      appendMessage(pins);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: motorMove");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(MOTOR_MOVE); // msgType = 50
      appendMessage(deviceId);
      appendMessage(pwr);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: motorMoveTo");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1); // size
      appendMessage(MOTOR_MOVE_TO); // msgType = 51
      appendMessage(deviceId);
      appendMessage(pos);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: encoderAttach");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1 + 1 + 1); // size
      appendMessage(ENCODER_ATTACH); // msgType = 52
      appendMessage(deviceId);
      appendMessage(type);
      appendMessage(pin);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: setZeroPoint");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(SET_ZERO_POINT); // msgType = 53
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    log.info("Sending Messge: servoStop");
    try {
      startMessage();
      appendMessage(MAGIC_NUMBER);
      appendMessage(1 + 1); // size
      appendMessage(SERVO_STOP); // msgType = 56
      appendMessage(deviceId);
 
      byte[] message = sendMessage();
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
    // TODO: This is a debug message only...
    String byteString = StringUtil.byteArrayToIntString(bytes);
    log.info("onBytes called byteCount: {} data: >{}<", byteCount, byteString);
    // this gives us the current full buffer that was read from the seral
    for (int i = 0 ; i < bytes.length; i++) {
      // For now, let's just call onByte for each byte upcasted as an int.
      Integer newByte = bytes[i] & 0xFF;
      try {
        /**
         * Archtype InputStream read - rxtxLib does not have this straightforward
         * design, but the details of how it behaves is is handled in the Serial
         * service and we are given a unified interface
         *
         * The "read()" is data taken from a blocking queue in the Serial service.
         * If we want to support blocking functions in Arduino then we'll
         * "publish" to our local queues
         */
        // TODO: consider reading more than 1 byte at a time ,and make this
        // callback onBytes or something like that.
        byteCount.incrementAndGet();
        
        log.info("{} Byte Count {} MsgSize: {} On Byte: {}", i, byteCount, msgSize, newByte);
        // ++byteCount;
        if (log.isDebugEnabled()) {
          log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
        }
        if (byteCount.get() == 1) {
          if (newByte != MAGIC_NUMBER) {
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
            // warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++errorServiceToHardwareRxCnt));
            log.warn("Arduino->MRL error - bad magic number {} - {} rx errors", newByte, ++errorServiceToHardwareRxCnt);
            // dump.setLength(0);
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
          // dump.append(String.format("MSG|SZ %d", msgSize));
        } else if (byteCount.get() == 3) {
          // This is the method..
          int method = newByte.intValue();
          // TODO: lookup the method in the label.. 
          if (!clearToSend) {
            // The only method we care about is begin!!!
            if (method != Msg.PUBLISH_MRL_COMM_BEGIN) {
              // This is a reset sort of scenario!  we should be killing our parser state
              // we are only looking for a begin message now!!
              byteCount = new AtomicInteger(0);
              msgSize = 0;
              continue;
            } else {
              // we're good to go.. maybe even clear to send at this point?
            }
          }
          if (methodToString(method).startsWith("ERROR")) {
            // we've got an error scenario here.. reset the parser and try again!
            log.error("Arduino->MRL error unknown method error. resetting parser.");
            byteCount = new AtomicInteger(0);
            msgSize = 0;
            if (isFullMessage(bytes)) {
              // TODO: This could be an infinite loop 
              // try to reprocess this byte array, maybe the parser got out of sync
              onBytes(bytes);
              return;
            }
            
          } else {
            ioCmd[byteCount.get() - 3] = method;
          }
        } else if (byteCount.get() > 3) {
          // remove header - fill msg data - (2) headbytes -1
          // (offset)
          // dump.append(String.format("|P%d %d", byteCount,
          // newByte));
          ioCmd[byteCount.get() - 3] = newByte.intValue();
        } else {
          // the case where byteCount is negative?! not got.
          log.warn("MRL error rx zero/negative size error: {} {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()));
          //error(String.format("Arduino->MRL error %d rx negsz errors", ++errorServiceToHardwareRxCnt));
          continue;
        }
        if (byteCount.get() == 2 + msgSize) {
          // we've received a full message
          int[] actualCommand = Arrays.copyOf(ioCmd, byteCount.get()-2);
          log.info("Full message received: {} Data:{}", VirtualMsg.methodToString(ioCmd[0]), actualCommand);
          // TODO: should we truncate our ioCmd that we send here?  the ioCmd array is larger than the message in almost all cases.
          // re-init the parser
          // TODO: this feels very thread un-safe!
          Arrays.fill(ioCmd, 0); // optimize remove
          // process the command.
          
          // This full command that we received. 
          
          processCommand(actualCommand);
          // we should only process this command if we are clear to sync.. 
          // if this is a begin command..  
//          if (!clearToSend) {
//            // if we're not clear to send.. we need to process this command
//            // only if it's a begin command.
//            if (isMrlCommBegin(actualCommand)) {
//              processCommand(actualCommand);
//            } else {
//              // reset the parser and attempt from the next byte
//              // TODO: check that it's not bytes.length-1
//              byte[] shiftedBytes = Arrays.copyOfRange(bytes, 1, bytes.length);
//              byteCount = new AtomicInteger(0);
//              onBytes(shiftedBytes);
//              return;
//            }
//            
//          } else {
//            processCommand(actualCommand);
//          }
          msgSize = 0;
          byteCount = new AtomicInteger(0);
          // Our 'first' getBoardInfo may not receive a acknowledgement
          // so this should be disabled until boadInfo is valid
          // clean up memory/buffers
          
        }
      } catch (Exception e) {
        ++errorHardwareToServiceRxCnt ;
        // error("msg structure violation %d", errorHardwareToServiceRxCnt);
        log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount.get()), e);
        // try again (clean up memory buffer)
        // Logging.logError(e);
        
        // perhpas we could find the first occurance of 170.. and then attempt to re-parse at that point.
        // find the first occurance of 170 in the bytes
        // subbytes
        // Maybe we can just walk the iterater back to the beginning based on the byte count .. and advance it by 1.. and continue.
        i = i - byteCount.get()+1;
        log.error("Trying to resume parsing the byte stream at position {} bytecount: {}", i, byteCount);
        log.error("Original Byte Array: {}", StringUtil.byteArrayToIntString(bytes));
        System.err.println("Try to consume more messages!");
        msgSize = 0;
        byteCount = new AtomicInteger(0);
        // attempt to reprocess from the beginngin
        // TODO: what about infinite loops!!!
        i = 0;
        return;
        
        
      }
    }
    log.info("Done with onBytes method.");
    return;
  }

  String F(String msg) {
    return msg;
  }
  
  public void publishError(String error) {
    log.error(error);
  }
  
  void appendMessage(int b8) throws Exception {

    if ((b8 < 0) || (b8 > 255)) {
      log.error("writeByte overrun - should be  0 <= value <= 255 - value = {}", b8);
    }

        baos.write(b8 & 0xFF);
//    serial.write(b8 & 0xFF);
  }
  
  void startMessage() {
    baos = new ByteArrayOutputStream();
  }

  void appendMessagebool(boolean b1) throws Exception {
    if (b1) {
      appendMessage(1);
    } else {
      appendMessage(0);
    }
  }

  void appendMessageb16(int b16) throws Exception {
    if ((b16 < -32768) || (b16 > 32767)) {
      log.error("writeByte overrun - should be  -32,768 <= value <= 32,767 - value = {}", b16);
    }

    appendMessage(b16 >> 8 & 0xFF);
    appendMessage(b16 & 0xFF);
  }

  void appendMessageb32(int b32) throws Exception {
    appendMessage(b32 >> 24 & 0xFF);
    appendMessage(b32 >> 16 & 0xFF);
    appendMessage(b32 >> 8 & 0xFF);
    appendMessage(b32 & 0xFF);
  }
  
  void appendMessagef32(float f32) throws Exception {
    //  int x = Float.floatToIntBits(f32);
    byte[] f = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(f32).array();
    appendMessage(f[3] & 0xFF);
    appendMessage(f[2] & 0xFF);
    appendMessage(f[1] & 0xFF);
    appendMessage(f[0] & 0xFF);
  }
  
  void appendMessagebu32(long b32) throws Exception {
    appendMessage((int)(b32 >> 24 & 0xFF));
    appendMessage((int)(b32 >> 16 & 0xFF));
    appendMessage((int)(b32 >> 8 & 0xFF));
    appendMessage((int)(b32 & 0xFF));
  }

  void appendMessage(String str) throws Exception {
    appendMessage(str.getBytes());
  }

  void appendMessage(int[] array) throws Exception {
    // write size
    appendMessage(array.length & 0xFF);

    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(array[i] & 0xFF);
    }
  }

  void appendMessage(byte[] array) throws Exception {
    // write size
    appendMessage(array.length);

    // write data
    for (int i = 0; i < array.length; ++i) {
      appendMessage(array[i]);
    }
  }
  
  synchronized byte[] sendMessage() throws Exception {
    byte[] message = baos.toByteArray();
    if (ackEnabled) {
      // wait for any outstanding pending messages.
      while (pendingMessage) {
        Thread.sleep(1);
        log.info("Pending message");
      }
      // set a new pending flag.
      pendingMessage=true;
    }
    // write data if serial not null.
    if (serial != null) {
      serial.write(message);
    }
    return message;
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
    case 9 :  {
      return "Encoder";

    }
    
    default: {
      return "unknown";
    }
    }
  }
  
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
        // part of resetting ?
        // ackRecievedLock.acknowledged = true;
        arduino.invoke("noAck");
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
      
      MrlCommListener arduino = (MrlCommListener)Runtime.start("arduino","MrlCommListener");
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

  public void waitForBegin() {
    // poll until a begin MrlComm Message has been seen.
    log.info("Wait for Begin called in Msg.");
    while (!clearToSend ) {
      // TODO: don't sleep. rather notify
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        log.info("Wait for MrlCommBegin interrupted.", e);
      }
    }
  }

  public synchronized void onConnect(String portName) {
    // reset the parser...
    log.info("On Connect Called in Msg.");
    this.byteCount = new AtomicInteger(0);
    this.msgSize = 0;
    // we're not clear to send.
    this.clearToSend = false;
    // watch for the first MrlCommBegin message;
    // TODO: we should have some sort of timeout / error handling here.
    this.waitForBegin();
    
  }


  private boolean isMrlCommBegin(int[] actualCommand) {
    // TODO Auto-generated method stub
    int method = actualCommand[0];
    if (Msg.PUBLISH_MRL_COMM_BEGIN == method) {
      return true;
    }
    return false;
  }

  private boolean isFullMessage(byte[] bytes) {
    // Criteria that a sequence of bytes could be parsed as a complete message.
    // can't be null
    if (bytes == null) 
      return false;
    // it's got to be at least 3 bytes long.  magic + method + size
    if (bytes.length <= 2) 
      return false;
    // first byte has to be magic
    if ((bytes[0] & 0xFF) != this.MAGIC_NUMBER) 
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

}
