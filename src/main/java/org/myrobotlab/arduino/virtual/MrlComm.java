package org.myrobotlab.arduino.virtual;

import static org.myrobotlab.arduino.VirtualMsg.MRLCOMM_VERSION;

import java.util.Iterator;
import java.util.LinkedList;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.VirtualArduino;
import org.python.jline.internal.Log;

  ///////////// MrlComm.h ///////////////
  // forward defines to break circular dependency
  // class Device;
  // class Msg;
  // class MrlComm;
  // class Pin;

  /***********************************************************************
   * Class MrlComm - This class represents the Arduino service as a device. It
   * can hosts devices such as Motors, Servos, Steppers, Sensors, etc. You can
   * dynamically add or remove devices, and the deviceList should be in synch
   * with the Java-Land deviceList. It has a list of pins which can be read from
   * or written to. It also follows some of the same methods as the Device in
   * Device.h It has an update() which is called each loop to do any necessary
   * processing
   * 
   */
public class MrlComm {

  public final BoardInfo boardInfo = new BoardInfo();


  /**
   * "global var"
   */
  // The mighty device List. This contains all active devices that are attached
  // to the arduino.
  LinkedList<Device> deviceList = new LinkedList<Device>();

  // list of pins currently being read from - can contain both digital and
  // analog
  public LinkedList<Pin> pinList = new LinkedList<Pin>();

  char[] config;
  // performance metrics and load timing
  // global debug setting, if set to true publishDebug will write to the serial
  // port.
  int byteCount;
  int msgSize;

 // last time board info was published
  long lastBoardInfoUs;

  boolean boardStatusEnabled;

  long lastHeartbeatUpdate;

  int[] customMsgBuffer = new int[VirtualMsg.MAX_MSG_SIZE];
  int customMsgSize;

  // handles all messages to and from pc
  transient VirtualMsg msg;

  boolean heartbeatEnabled;

  public
  // utility methods
  // int getFreeRam();
  // Device getDevice(int id);

  boolean ackEnabled = true;

  // Device addDevice(Device device);
  // void update();

    // Below are generated callbacks controlled by
    // arduinoMsgs.schema
    // <generatedCallBacks>
	// > getBoardInfo
	//void getBoardInfo();
	// > enablePin/address/type/b16 rate
	//void enablePin( byte address,  byte type,  int rate);
	// > setDebug/bool enabled
	//void setDebug( boolean enabled);
	// > setSerialRate/b32 rate
	//void setSerialRate( long rate);
	// > softReset
	//void softReset();
	// > enableAck/bool enabled
	//void enableAck( boolean enabled);
	// > echo/f32 myFloat/myByte/f32 secondFloat
	//void echo( float myFloat,  byte myByte,  float secondFloat);
	// > controllerAttach/serialPort
	//void controllerAttach( byte serialPort);
	// > customMsg/[] msg
	//void customMsg( byte msgSize, const byte*msg);
	// > deviceDetach/deviceId
	//void deviceDetach( byte deviceId);
	// > i2cBusAttach/deviceId/i2cBus
	//void i2cBusAttach( byte deviceId,  byte i2cBus);
	// > i2cRead/deviceId/deviceAddress/size
	//void i2cRead( byte deviceId,  byte deviceAddress,  byte size);
	// > i2cWrite/deviceId/deviceAddress/[] data
	//void i2cWrite( byte deviceId,  byte deviceAddress,  byte dataSize, const byte*data);
	// > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
	//void i2cWriteRead( byte deviceId,  byte deviceAddress,  byte readSize,  byte writeValue);
	// > neoPixelAttach/deviceId/pin/b32 numPixels
	//void neoPixelAttach( byte deviceId,  byte pin,  long numPixels);
	// > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
	//void neoPixelSetAnimation( byte deviceId,  byte animation,  byte red,  byte green,  byte blue,  int speed);
	// > neoPixelWriteMatrix/deviceId/[] buffer
	//void neoPixelWriteMatrix( byte deviceId,  byte bufferSize, const byte*buffer);
	// > disablePin/pin
	//void disablePin( byte pin);
	// > disablePins
	//void disablePins();
	// > setTrigger/pin/triggerValue
	//void setTrigger( byte pin,  byte triggerValue);
	// > setDebounce/pin/delay
	//void setDebounce( byte pin,  byte delay);
	// > servoAttach/deviceId/pin/b16 initPos/b16 initVelocity
	//void servoAttach( byte deviceId,  byte pin,  int initPos,  int initVelocity);
	// > servoAttachPin/deviceId/pin
	//void servoAttachPin( byte deviceId,  byte pin);
	// > servoDetachPin/deviceId
	//void servoDetachPin( byte deviceId);
	// > servoSetMaxVelocity/deviceId/b16 maxVelocity
	//void servoSetMaxVelocity( byte deviceId,  int maxVelocity);
	// > servoSetVelocity/deviceId/b16 velocity
	//void servoSetVelocity( byte deviceId,  int velocity);
	// > servoSweepStart/deviceId/min/max/step
	//void servoSweepStart( byte deviceId,  byte min,  byte max,  byte step);
	// > servoSweepStop/deviceId
	//void servoSweepStop( byte deviceId);
	// > servoMoveToMicroseconds/deviceId/b16 target
	//void servoMoveToMicroseconds( byte deviceId,  int target);
	// > servoSetAcceleration/deviceId/b16 acceleration
	//void servoSetAcceleration( byte deviceId,  int acceleration);
	// > serialAttach/deviceId/relayPin
	//void serialAttach( byte deviceId,  byte relayPin);
	// > serialRelay/deviceId/[] data
	//void serialRelay( byte deviceId,  byte dataSize, const byte*data);
	// > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
	//void ultrasonicSensorAttach( byte deviceId,  byte triggerPin,  byte echoPin);
	// > ultrasonicSensorStartRanging/deviceId
	//void ultrasonicSensorStartRanging( byte deviceId);
	// > ultrasonicSensorStopRanging/deviceId
	//void ultrasonicSensorStopRanging( byte deviceId);
        // > setAref/b16 aref
        //void setAref( int aref);
    // </generatedCallBacks>
    // end
/*
 
  public:
    unsigned long loopCount; // main loop count
    MrlComm();
    ~MrlComm();
    void publishBoardStatus();
    void publishVersion();
    void publishBoardInfo();
    void processCommand();
    void processCommand(int ioType);
    void updateDevices();
    unsigned int getCustomMsg();
    int getCustomMsgSize();
    void begin(HardwareSerial& serial);
    bool readMsg();
};
  
#endif
*/


  public long loopCount; // main loop count

  ///////////// utility/support methods begin /////////////
  String F(String r) {
    return r;
  }

  java.lang.String String(long x) {
    return new String("" + x);
  }

  java.lang.String String(String x) {
    return x;
  }

  java.lang.String String(float x) {
    return new String("" + x);
  }

  java.lang.String String(int x) {
    return new String("" + (x & 0xFF));
  }

  public void pinMode(int address, int input) {
    // TODO change mode of pin ... duh
  }

  public long micros() {
    return System.nanoTime() / 1000;
  }

  static public int getRandom(int min, int max) {
    return min + (int) (Math.random() * ((max - min) + 1));
  }

  private int digitalRead(int address) {
    return getRandom(0, 1);
  }

  private int analogRead(int address) {
    /* to simulate longer analogReads :)
    try{
    Thread.sleep(45);
    } catch(Exception e){}
    */
    return getRandom(0, 1024);
  }

  private long millis() {
    return System.currentTimeMillis();
  }
  

  ///////////// support methods end ///////////////

  ///////////// MrlCom.cpp ////////////////

  /**
   * <pre>
   Schema Type Conversions
  
   Schema         Arduino                         Java                              Range
   none           byte/unsigned char              int (cuz Java byte bites)         1 byte - 0 to 255
   boolean        boolean                         boolean                           0 1
   b16            int                             int (short)                       2 bytes	-32,768 to 32,767
   b32            long                            int                               4 bytes -2,147,483,648 to 2,147,483, 647
   bu32           unsigned long                   long                              0 to 4,294,967,295
   str            char*, size                     String                            variable length
   []             byte[], size                    int[]                             variable length
   f32            float                           double                            4 bytes
   * </pre>
   */

  VirtualArduino virtual;


  public MrlComm(VirtualArduino virtual) {
    // msg = VirtualMsg.getInstance();
    this.virtual = virtual;
    msg = new VirtualMsg(this, virtual.getSerial());
    softReset();
  }

  // ~MrlComm() {
  // }

 
  int getFreeRam() {
    // KW: In the future the arduino might have more than an 32/64k of ram. an
    // int might not be enough here to return.
    // extern int __heap_start, *__brkval;
    // int v;
    // return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
    return 940 - (deviceList.size() * 20) - getRandom(0, 20); // TODO add size of device list
  }

  /**
   * Get a device given it's id.
   * 
   * @param id - the device id to fetch.  (I think this is the internal device id as it is the index into the mrl device list.
   * @return - a device
   */
  public Device getDevice(int id) {
    // ListNode<Device>node = deviceList.getRoot();
    Iterator<Device> i = deviceList.iterator();
    while (i.hasNext()) {
      Device node = i.next();
      if (node.id == id) {
        return node;
      }
      // node = node.next;
    }

    msg.publishError(F("device does not exist"));
    return null; // returning a null ptr can cause runtime error
    // you'll still get a runtime error if any field, member or method not
    // defined is accessed
  }
  /**
   * This adds a device to the current set of active devices in the deviceList.
   * 
   * FIXME - G: I think dynamic array would work better at least for the
   * deviceList TODO: KW: i think it's pretty dynamic now. G: the nextDeviceId &
   * Id leaves something to be desired - and the "index" does not spin through
   * the deviceList to find it .. a dynamic array of pointers would only expand
   * if it could not accomidate the current number of devices, when a device was
   * removed - the slot could be re-used by the next device request
   */
  Device addDevice(Device device) {
    deviceList.add(device);
    return device;
  }

  /***********************************************************************
   * UPDATE DEVICES BEGIN updateDevices updates each type of device put on the
   * device list depending on their type. This method processes each loop.
   * Typically this "back-end" processing will read data from pins, or change
   * states of non-blocking pulses, or possibly regulate a motor based on pid
   * values read from pins
   */
  public void updateDevices() {
 
    // update self - the first device which
    // is type Arduino
    update();

    // iterate through our device list and call update on them.
    for (int i = 0; i < deviceList.size(); ++i) {
      Device node = deviceList.get(i);
      node.update();
    }
  }

  /***********************************************************************
   * UPDATE BEGIN updates self - reads from the pinList both analog and digital
   * sends pin data back
   */
  public void update() {
    // this counts cycles of updates
    // until it is reset after sending publishBoardInfo
    ++loopCount;
    long now = millis();
    if ((now - lastHeartbeatUpdate > 1000)&& heartbeatEnabled) {
      onDisconnect();
      lastHeartbeatUpdate = now;
      heartbeatEnabled = false;
      return;
    }

    if (pinList.size() > 0) {

      // size of payload - 1 int for address + 2 bytes per pin read
      // this is an optimization in that we send back "all" the read pin
      // data in a
      // standard 2 int package - digital reads don't need both bytes,
      // but the
      // sending it all back in 1 msg and the simplicity is well worth it
      // msg.addData(pinList.size() * 3 /* 1 address + 2 read bytes */);

      int[] buffer = new int[pinList.size() * 3];

      // iterate through our device list and call update on them.
      boolean dataCount = false;
      for (int i = 0; i < pinList.size(); ++i) {
        Pin pin = pinList.get(i);
        if (pin.rate == 0 || (now > pin.lastUpdate + (1000 / pin.rate))) {
          pin.lastUpdate = now;
          // TODO: move the analog read outside of this method and
          if (pin.type == Arduino.ANALOG) {
            pin.value = analogRead(pin.address);
          } else {
            pin.value = digitalRead(pin.address);
          }

          // loading both analog & digital data
          buffer[3 * i] = pin.address;// 1 int
          buffer[3 * i + 1] = pin.value >> 8 & 0xFF;// 2 int b16
          // value
          buffer[3 * i + 2] = pin.value & 0xFF;// 2 int b16 value
          // ++dataCount;
          dataCount = true;
        }
        // node = node.next;
      }
      if (dataCount) {
        msg.publishPinArray(buffer);
      }
    }
  }

  int getCustomMsgSize() {
    return customMsgSize;
  }

  public void processCommand() {

    msg.processCommand();
    if (ackEnabled) {
      msg.publishAck(msg.getMethod());
    }
  }

  public void enableAck(boolean enabled) {
    ackEnabled = enabled;
  }

  boolean readMsg() throws Exception {
    return msg.readMsg();
  }

  public void publishError(java.lang.String f) {
    msg.publishMRLCommError(f);
  }

  // void begin(HardwareSerial& serial) {
  void begin(HardwareSerial serial) {

    // TODO: the arduino service might get a few garbage bytes before we're able
    // to run, we should consider some additional logic here like a
    // "publishReset"
    // publish version on startup so it's immediately available for mrl.
    // TODO: see if we can purge the current serial port buffers

    while (!serial.ready()) {
      ; // wait for serial port to connect. Needed for native USB
    }

    // clear serial
    serial.flush();

    // msg.begin(serial);

    // send 3 boardInfos to PC to announce,
    // Hi I'm an Arduino with version x, board type y, and I'm ready :)
    for (int i = 0; i < 5; ++i) {
      publishBoardInfo();
      serial.flush();
    }
  }

 /***********************************************************************
   * PUBLISH_BOARD_INFO This function updates the average time it took to run
   * the main loop and reports it back with a publishBoardStatus MRLComm message
   *
   * TODO: avgTiming could be 0 if loadTimingModule = 0 ?!
   *
   * MAGIC_NUMBER|7|[loadTime long0,1,2,3]|[freeMemory int0,1]
   */


  public void publishBoardInfo() {

    int[] deviceSummary = new int[deviceList.size() * 2];
    for (int i = 0; i < deviceList.size(); ++i) {
      deviceSummary[i] = deviceList.get(i).id;
      deviceSummary[i + 1] = deviceList.get(i).type;
    }

    long now = micros();
    int load = (int)((now - lastBoardInfoUs)/loopCount);
    msg.publishBoardInfo(MRLCOMM_VERSION, Arduino.BOARD_TYPE_ID_UNO, load, getFreeRam(), pinList.size(), deviceSummary);
    lastBoardInfoUs = now;
    loopCount = 0;
  }

  /****************************************************************
   * GENERATED METHOD INTERFACE BEGIN All methods signatures below this line are
   * controlled by arduinoMsgs.schema The implementation contains custom logic -
   * but the signature is generated
   *
   */

  // > getBoardInfo
  public void getBoardInfo() {
    // msg.publishBoardInfo(MRLCOMM_VERSION, BOARD);
    publishBoardInfo();
  }

  // > echo/str name1/b8/bu32 bui32/b32 bi32/b9/str name2/[] config/bu32 bui322
  public void echo(float myFloat, int myByte, float mySecondFloat) {
    msg.publishDebug(String("echo float " + String(myFloat)));
    msg.publishDebug(String("echo int " + String(myByte)));
    msg.publishDebug(String("echo float2 " + String(mySecondFloat)));
    // msg.publishDebug(String("pi is " + String(3.141529)));
    msg.publishEcho(myFloat, myByte & 0xFF, mySecondFloat);
  }

  // > customMsg/[] msg
  // from PC -. loads customMsg buffer
  public void customMsg(int[] msg) {
    for (int i = 0; i < msgSize && msgSize < 64; i++) {
      customMsgBuffer[i] = msg[i]; // *(msg + i);
    }
    customMsgSize = msgSize;
  }

  /**
   * deviceDetach - get the device if it exists delete it and remove it from the
   * deviceList
   * @param deviceId int for the device id
   */
  // > deviceDetach/deviceId
  public void deviceDetach(Integer deviceId) {
    for (int i = 0; i < deviceList.size(); ++i) {
      if (deviceList.get(i).id == deviceId) {
        deviceList.remove(i);
        return;
      }
    }
  }

  // > disablePin/pin
  public void disablePin(Integer pinAddress) {
    for (int i = 0; i < pinList.size(); ++i) {
      Pin pin = pinList.get(i);
      if (pin.address == pinAddress) {
        pinList.remove(i);
        return;
      }
    }
  }

  // > disablePins
  public void disablePins() {
    while (pinList.size() > 0) {
      // delete pinList.pop();
      pinList.pop();
    }
  }


  // > enablePin/address/type/b16 rate
  public void enablePin(int address, int type, int rate) {
    // don't add it twice
    for (int i = 0; i < pinList.size(); ++i) {
      Pin pin = pinList.get(i);
      if (pin.address == address) {
        // TODO already exists error?
        return;
      }
    }

    if (type == Pin.DIGITAL) {
      pinMode(address, Pin.INPUT);
    }
    Pin p = new Pin(address, type, rate);
    p.lastUpdate = 0;
    pinList.add(p);
  }


  // > i2cBusAttach/deviceId/i2cBus
  public void i2cBusAttach(int deviceId, int i2cBus) {
    MrlI2CBus i2cbus = (MrlI2CBus) addDevice(new MrlI2CBus(deviceId, virtual));
    i2cbus.attach(i2cBus);
  }

  // > i2cRead/deviceId/deviceAddress/size
  public void i2cRead(int deviceId, int deviceAddress, int size) {
    ((MrlI2CBus) getDevice(deviceId)).i2cRead(deviceAddress, size);
  }

  // > i2cWrite/deviceId/deviceAddress/[] data
  public void i2cWrite(int deviceId, int deviceAddress, int[] data) {
    ((MrlI2CBus) getDevice(deviceId)).i2cWrite(deviceAddress, data.length, data);
  }

  // > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
  public void i2cWriteRead(int deviceId, int deviceAddress, int readSize, int writeValue) {
    ((MrlI2CBus) getDevice(deviceId)).i2cWriteRead(deviceAddress, readSize, writeValue);
  }

  // > neoPixelAttach/pin/b16 numPixels
  public void neoPixelAttach(int deviceId, int pin, long numPixels) {
    // msg.publishDebug("MrlNeopixel.deviceAttach!");

    MrlNeopixel neo = (MrlNeopixel) addDevice(new MrlNeopixel(deviceId, virtual));
    msg.publishDebug("id" + String(deviceId));
    neo.attach(pin, numPixels);
  }

  // > neoPixelAttach/pin/b16 numPixels
  public void neoPixelSetAnimation(int deviceId, int animation, int red, int green, int blue, int speed) {
    msg.publishDebug("MrlNeopixel.setAnimation!");
    ((MrlNeopixel) getDevice(deviceId)).setAnimation(animation, red, green, blue, speed);
  }

  // > neoPixelWriteMatrix/deviceId/[] buffer
  public void neoPixelWriteMatrix(int deviceId, int[] buffer) {
    ((MrlNeopixel) getDevice(deviceId)).neopixelWriteMatrix(buffer.length, buffer);
  }

  // > servoAttach/deviceId/pin/targetOutput/b16 velocity
  public void servoAttach(int deviceId, int pin, int initialPosUs, int velocity, String name) {
    MrlServo servo = new MrlServo(deviceId, virtual);
    addDevice(servo);
    // not your mama's attach - this is attaching/initializing the MrlDevice
    servo.attach(pin, initialPosUs, velocity, name);
  }

  // > servoEnablePwm/deviceId/pin
  public void servoAttachPin(int deviceId, int pin) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.attachPin(pin);
  }

  // > servoDisablePwm/deviceId
  public void servoDetachPin(int deviceId) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.detachPin();
  }

  // > servoSetVelocity/deviceId/b16 velocity
  public void servoSetVelocity(int deviceId, int velocity) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.setVelocity(velocity);
  }

  public void servoSetAcceleration(int deviceId, int acceleration) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.setAcceleration(acceleration);
  }

  public void servoSweepStart(int deviceId, int min, int max, int step) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.startSweep(min, max, step);
  }

  public void servoSweepStop(int deviceId) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.stopSweep();
  }

  public void servoMoveToMicroseconds(int deviceId, int target) {
    MrlServo servo = (MrlServo) getDevice(deviceId);
    servo.moveToMicroseconds(target);
  }

  public void setDebug(boolean enabled) {
    msg.debug = enabled;
  }

  public void setSerialRate(long rate) {
    msg.publishDebug("setSerialRate " + String(rate));
  }

  // TODO - implement
  // > setTrigger/pin/value
  public void setTrigger(int pin, int triggerValue) {
    msg.publishDebug("implement me ! setDebounce (" + String(pin) + "," + String(triggerValue));
  }

  // TODO - implement
  // > setDebounce/pin/delay
  public void setDebounce(int pin, int delay) {
    msg.publishDebug("implement me ! setDebounce (" + String(pin) + "," + String(delay));
  }

  // TODO - implement
  // > serialAttach/deviceId/relayPin
  public void serialAttach(int deviceId, int relayPin) {
    MrlSerialRelay relay = new MrlSerialRelay(deviceId);
    addDevice(relay);
    relay.attach(relayPin);
  }

  // TODO - implement
  // > serialRelay/deviceId/[] data
  public void serialRelay(int deviceId, int[] data) {
    MrlSerialRelay relay = (MrlSerialRelay) getDevice(deviceId);
    // msg.publishDebug("serialRelay (" + String(dataSize) + "," +
    // String(deviceId));
    relay.write(data, data.length);
  }

  // > softReset
  public void softReset() {
    // removing devices & pins
    deviceList.clear();
    
    pinList.clear();

    // resetting variables to default
    loopCount = 0;
    boardStatusEnabled = false;
    msg.debug = false;
    lastHeartbeatUpdate = 0;
    for (int i = 0; i < VirtualMsg.MAX_MSG_SIZE; i++) {
      customMsgBuffer[i] = 0;
    }
    customMsgSize = 0;
    heartbeatEnabled = true;
  }

  // > ultrasonicSensorAttach/deviceId/triggerPin/echoPin
  public void ultrasonicSensorAttach(int deviceId, int triggerPin, int echoPin) {
    MrlUltrasonicSensor sensor = (MrlUltrasonicSensor) addDevice(new MrlUltrasonicSensor(deviceId, virtual));
    sensor.attach(triggerPin, echoPin);
  }

  // > ultrasonicSensorStartRanging/deviceId
  public void ultrasonicSensorStartRanging(int deviceId) {
    MrlUltrasonicSensor sensor = (MrlUltrasonicSensor) getDevice(deviceId);
    sensor.startRanging();
  }

  // > ultrasonicSensorStopRanging/deviceId
  public void ultrasonicSensorStopRanging(int deviceId) {
    MrlUltrasonicSensor sensor = (MrlUltrasonicSensor) getDevice(deviceId);
    sensor.stopRanging();
  }

  int getCustomMsg() {
    if (customMsgSize == 0) {
      return 0;
    }
    int retval = customMsgBuffer[0];
    for (int i = 0; i < customMsgSize - 1; i++) {
      customMsgBuffer[i] = customMsgBuffer[i + 1];
    }
    customMsgBuffer[customMsgSize] = 0;
    customMsgSize--;
    return retval;
  }

 void onDisconnect() {
   Iterator<Device> i = deviceList.iterator();
   while (i.hasNext()) {
     Device node = i.next();
     node.onDisconnect();
     // node = node.next;
   }
  boardStatusEnabled = false;
 }

 void sendCustomMsg(int[] customMsg) {
  msg.publishCustomMsg(customMsg);
 }
 
 // > motorAttach/deviceId/type/[] pins
 public void motorAttach(Integer deviceId, Integer type, int[] pins) {
   MrlMotor servo = new MrlMotor(deviceId, virtual);
   addDevice(servo);
   // not your mama's attach - this is attaching/initializing the MrlDevice
   // servo.attach(type, initialPosUs, velocity, name);
 }

 // > motorMove/deviceId/pwr
 public void motorMove(Integer deviceId, Integer pwr) {
   MrlMotor motor = (MrlMotor) getDevice(deviceId);
   motor.move(pwr);
 }

 // > motorMoveTo/deviceId/pos
 public void motorMoveTo(Integer deviceId, Integer pos) {
   MrlMotor motor = (MrlMotor) getDevice(deviceId);
   motor.moveTo(pos);
 }


  /*
  public void setBoardType(String board) {
    boardInfo.setType(board);
  }
  */

  public void invoke(String method, Object... params) {
    virtual.invokeOn(this, method, params);
  }

  public void analogWrite(Integer pin, Integer value) {
    // TODO Auto-generated method stub

  }

  public void digitalWrite(Integer pin, Integer value) {
    // TODO Auto-generated method stub

  }

  public String getName() {
    return virtual.getName();
  }

  public VirtualMsg getMsg() {
    return msg;
  }

  public void begin(org.myrobotlab.service.Serial serial) {
    
  }
  
  // > enablePin/address/type/b16 rate
  public void setAref(int aref) {
    // TODO
	 // msg.setAref(aref);
  }


}
