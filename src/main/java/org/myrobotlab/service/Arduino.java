package org.myrobotlab.service;

import static org.myrobotlab.arduino.Msg.MAGIC_NUMBER;
import static org.myrobotlab.arduino.Msg.MAX_MSG_SIZE;
import static org.myrobotlab.arduino.Msg.MRLCOMM_VERSION;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.arduino.ArduinoUtils;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.arduino.DeviceSummary;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.i2c.I2CBus;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.abstracts.AbstractMicrocontroller;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinArrayPublisher;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.UltrasonicSensorControl;
import org.myrobotlab.service.interfaces.UltrasonicSensorController;
import org.slf4j.Logger;


public class Arduino extends AbstractMicrocontroller
    implements I2CBusController, I2CController, SerialDataListener, ServoController, MotorController, NeoPixelController, UltrasonicSensorController, PortConnector, RecordControl,
    /* SerialRelayListener, */PortListener, PortPublisher, EncoderController, PinArrayPublisher {

  transient public final static Logger log = LoggerFactory.getLogger(Arduino.class);

  public static class I2CDeviceMap {
    public String busAddress;
    public transient I2CControl control;
    public String deviceAddress;
  }

  public static class Sketch implements Serializable {
    private static final long serialVersionUID = 1L;
    public String data;
    public String name;

    public Sketch(String name, String data) {
      this.name = name;
      this.data = data;
    }
  }

  public static final int ANALOG = 1;

  public transient static final int BOARD_TYPE_ID_ADK_MEGA = 3;

  public transient static final int BOARD_TYPE_ID_MEGA = 1;
  public transient static final int BOARD_TYPE_ID_NANO = 4;
  public transient static final int BOARD_TYPE_ID_PRO_MINI = 5;
  public transient static final int BOARD_TYPE_ID_UNKNOWN = 0;
  public transient static final int BOARD_TYPE_ID_UNO = 2;

  public transient static final String BOARD_TYPE_MEGA = "mega.atmega2560";
  public transient static final String BOARD_TYPE_MEGA_ADK = "megaADK";
  public transient static final String BOARD_TYPE_NANO = "nano";
  public transient static final String BOARD_TYPE_PRO_MINI = "pro mini";
  public transient static final String BOARD_TYPE_UNO = "uno";
  public static final int DIGITAL = 0;

  public static final int INPUT = 0x0;
  public static final int MOTOR_BACKWARD = 0;

  public static final int MOTOR_FORWARD = 1;
  public static final int MOTOR_TYPE_DUAL_PWM = 2;

  public static final int MOTOR_TYPE_SIMPLE = 1;
  public static final int MRL_IO_NOT_DEFINED = 0;
  public static final int MRL_IO_SERIAL_0 = 1;
  public static final int MRL_IO_SERIAL_1 = 2;
  public static final int MRL_IO_SERIAL_2 = 3;

  public static final int MRL_IO_SERIAL_3 = 4;
  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   *
   * @return ServiceType - returns all the data
   *
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Arduino.class.getCanonicalName());
    meta.addDescription("controls an Arduino microcontroller as a slave, which allows control of all the devices the Arduino is attached to, such as servos, motors and sensors");
    meta.addCategory("microcontroller");
    meta.addPeer("serial", "Serial", "serial device for this Arduino");
    return meta;
  }

  /**
   * path of the Arduino IDE must be set by user should not be static - since
   * gson will not serialize it, and it won't be 'saved()'
   */
  public String arduinoPath;

  String aref;

  @Deprecated /*
               * should be just another attachable - this is a bad
               * implementation
               */
  transient Map<Integer, Arduino> attachedController = new ConcurrentHashMap<Integer, Arduino>();

  /**
   * board info "from" MrlComm - which can be different from what the user say's
   * it is - if there is a difference the "user" should be notified - but not
   * forced to use the mrlBoardInfo.
   */
  volatile BoardInfo boardInfo = null;

  volatile BoardInfo lastBoardInfo = null;

  boolean boardInfoEnabled = true;

  private long boardInfoRequestTs;

  int byteCount;
  @Deprecated /*
               * should develop a MrlSerial on Arduinos and
               * Arduino.getSerial("s1")
               */
  public transient int controllerAttachAs = MRL_IO_NOT_DEFINED;

  /**
   * id reference of sensor, key is the MrlComm device id
   */
  transient Map<Integer, DeviceMapping> deviceIndex = new ConcurrentHashMap<Integer, DeviceMapping>();

  /**
   * Devices - string name index of device we need 2 indexes for sensors because
   * they will be referenced by name OR by index
   */
  transient Map<String, DeviceMapping> deviceList = new ConcurrentHashMap<String, DeviceMapping>();

  // FIXME - use
  int errorServiceToHardwareRxCnt = 0;

  int errorHardwareToServiceRxCnt = 0;

  I2CBus i2cBus = null;

  volatile byte[] i2cData = new byte[64];

  /**
   * i2c This needs to be volatile because it will be updated in a different
   * threads
   */
  volatile boolean i2cDataReturned = false;

  volatile int i2cDataSize;

  Map<String, I2CDeviceMap> i2cDevices = new ConcurrentHashMap<String, I2CDeviceMap>();

  transient int[] ioCmd = new int[MAX_MSG_SIZE];

  @Deprecated /*
               * use attachables like everything else - power mapping should be
               * inside the motorcontrol
               */
  transient Mapper motorPowerMapper = new MapperLinear(-1.0, 1.0, -255.0, 255.0);

  public transient Msg msg;

  int msgSize;

  Integer nextDeviceId = 0;

  int numAck = 0;

  /**
   * Serial service - the Arduino's serial connection
   */
  transient Serial serial;

  /**
   * MrlComm sketch
   */
  public Sketch sketch;

  public String uploadSketchResult = "";

  transient private VirtualArduino virtual;

  int mrlCommBegin = 0;

  long syncStartTypeTs = System.currentTimeMillis();

  public Arduino(String n, String id) {
    super(n, id);

    // config - if saved is loaded - if not default to uno
    if (board == null) {
      board = "uno";
    }

    // board is set
    // now we can create a pin list
    getPinList();

    // get list of board types
    getBoardTypes();

    // FIXME - load from unzipped resource directory ? - no more jar access like
    // below
    String mrlcomm = FileIO.resourceToString("Arduino/MrlComm/MrlComm.ino");

    setSketch(new Sketch("MrlComm", mrlcomm));

    // add self as an attached device
    // to handle pin events
    attachDevice(this, (Object[]) null);
  }

  // > analogWrite/address/value
  public void analogWrite(int address, int value) {
    log.info("analogWrite({},{})", address, value);
    msg.analogWrite(address, value);
  }

  public void analogWrite(String pin, Integer value) {
    PinDefinition pinDef = getPin(pin);
    analogWrite(pinDef.getAddress(), value);
  }

  DeviceSummary[] arrayToDeviceSummary(int[] deviceSummary) {
    log.debug("mds - {}", Arrays.toString(deviceSummary));
    DeviceSummary[] ds = new DeviceSummary[deviceSummary.length];
    for (int i = 0; i < deviceSummary.length; i++) {
      int id = deviceSummary[i];
      DeviceSummary ds0 = new DeviceSummary(getDeviceName(id), id);
      ds[i] = ds0;
    }
    // log.error("ds - {}", Arrays.toString(ds));
    return ds;
  }

  /**
   * Routing Attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable service) throws Exception {
    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      attachServoControl((ServoControl) service);
      ((ServoControl) service).attach(this);
      return;
    } else if (MotorControl.class.isAssignableFrom(service.getClass())) {
      attachMotorControl((MotorControl) service);
      return;
    } else if (EncoderControl.class.isAssignableFrom(service.getClass())) {
      // need to determine the encoder type!
      attach((EncoderControl) service);
      return;
    }
    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    servo.setPin(pin);
    attachServoControl(servo);
  }

  /**
   * String interface - this allows you to easily use url api requests like
   * /attach/nameOfListener/3
   */
  public void attach(String listener, int address) {
    attach((PinListener) Runtime.getService(listener), address);
  }

  @Override
  public void attach(UltrasonicSensorControl sensor, Integer triggerPin, Integer echoPin) throws Exception {
    // refer to
    // http://myrobotlab.org/content/control-controller-manifesto
    if (isAttached(sensor)) {
      log.info("{} already attached", sensor.getName());
      return;
    }

    // critical init code
    DeviceMapping dm = attachDevice(sensor, new Object[] { triggerPin, echoPin });
    Integer deviceId = dm.getId();
    msg.ultrasonicSensorAttach(deviceId, triggerPin, echoPin);

    // call the other service's attach
    sensor.attach(this, triggerPin, echoPin);
  }

  synchronized private DeviceMapping attachDevice(Attachable device, Object[] attachConfig) {
    DeviceMapping map = new DeviceMapping(device, attachConfig);
    map.setId(nextDeviceId);
    deviceList.put(device.getName(), map);
    deviceIndex.put(nextDeviceId, map);
    ++nextDeviceId;
    // return map.getId();
    return map;
  }

  /**
   * Attach an encoder to the arduino
   * 
   * @param encoder
   *          - the encoder control to attach
   * @throws Exception
   */
  @Override
  public void attach(EncoderControl encoder) throws Exception {
    Integer deviceId = null;
    // send data to micro-controller

    // TODO: update this with some enum of various encoder types..
    // for now it's just AMT203 ...
    int type = 0;
    Integer address = null;
    if (encoder instanceof Amt203Encoder) {
      type = 0;
      address = getAddress(((Amt203Encoder) encoder).getPin());
    } else if (encoder instanceof As5048AEncoder) {
      type = 1;
      address = getAddress(((As5048AEncoder) encoder).getPin());
    } else {
      error("unknown encoder type {}", encoder.getClass().getName());
    }
    attachDevice(encoder, new Object[] { address }); // FIXME - don't know why
                                                     // this is necessary -
                                                     // Attachable is only
                                                     // needed
    msg.encoderAttach(deviceId, type, address);

    encoder.attach(this);

  }

  @Override
  public void attachI2CControl(I2CControl control) {
    // Create the i2c bus device in MrlComm the first time this method is
    // invoked.
    // Add the i2c device to the list of i2cDevices
    // Pattern: deviceAttach(device, Object... config)
    // To add the i2c bus to the deviceList I need an device that represents
    // the i2c bus here and in MrlComm
    // This will only handle the creation of i2cBus.
    if (i2cBus == null) {
      i2cBus = new I2CBus(String.format("I2CBus%s", control.getDeviceBus()));
      i2cBusAttach(i2cBus, Integer.parseInt(control.getDeviceBus()));
    }

    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    String key = String.format("%s.%s", control.getDeviceBus(), control.getDeviceAddress());
    I2CDeviceMap devicedata = new I2CDeviceMap();
    if (i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getDeviceBus(), control.getDeviceAddress(), control.getName());
    } else {
      devicedata.busAddress = control.getDeviceBus();
      devicedata.deviceAddress = control.getDeviceAddress();
      devicedata.control = control;
      i2cDevices.put(key, devicedata);
      control.attachI2CController(this);
    }
  }

  // @Override
  public void attachMotorControl(MotorControl motor) throws Exception {
    if (isAttached(motor)) {
      log.info("motor {} already attached", motor.getName());
      return;
    }

    Integer motorType = null;
    int[] pins = null;

    if (motor.getClass().equals(Motor.class)) {
      motorType = MOTOR_TYPE_SIMPLE;
      Motor m = (Motor) motor;
      pins = new int[] { getAddress(m.getPwrPin()), getAddress(m.getDirPin()) };
    } else if (motor.getClass().equals(MotorDualPwm.class)) {
      motorType = MOTOR_TYPE_DUAL_PWM;
      MotorDualPwm m = (MotorDualPwm) motor;
      pins = new int[] { getAddress(m.getLeftPwmPin()), getAddress(m.getRightPwmPin()) };
      // } else if (motor.getClass().equals(MotorStepper)){ // FIXME implement

    } else {
      throw new IOException(String.format("do not know how to attach Motor type %s", motor.getClass().getSimpleName()));
    }

    // this saves original "attach" configuration - and maintains internal
    // data
    // structures
    // and does DeviceControl.attach(this)
    DeviceMapping dm = attachDevice(motor, new Object[] { motorType, pins });
    Integer deviceId = dm.getId();

    // send data to micro-controller - convert degrees to microseconds
    // int uS = degreeToMicroseconds(targetOutput);
    msg.motorAttach(deviceId, motorType, pins);

    // the callback - motor better have a check
    // isAttached(MotorControl) to prevent infinite loop
    // motor.attach(this, pin, targetOutput, velocity);
    motor.attachMotorController(this);
  }

  @Override
  public void attachServoControl(ServoControl servo) {
    if (isAttached(servo)) {
      log.info("servo {} already attached", servo.getName());
      return;
    }

    int pin = getAddress(servo.getPin());
    // targetOutput is ALWAYS ALWAYS degrees
    double targetOutput = servo.getTargetOutput();
    double speed = (servo.getSpeed() == null) ? -1 : servo.getSpeed();

    // add a device to our deviceList
    DeviceMapping dm = attachDevice(servo, new Object[] { pin, targetOutput, speed });

    if (isConnected()) {
      int uS = degreeToMicroseconds(servo.getTargetOutput());
      msg.servoAttach(dm.getId(), pin, uS, (int) speed, servo.getName());
      if (servo.isEnabled()) {
        msg.servoAttachPin(dm.getId(), pin);
      }
    }
    servo.attach(this);
  }

  /**
   * 
   * @param dm
   */
  public void reattach(DeviceMapping dm) {
    
    Attachable attachable = dm.getDevice();
    if (attachable instanceof Servo) {
      Servo servo = (Servo) attachable;
      int uS = degreeToMicroseconds(servo.getTargetOutput());
      double speed = (servo.getSpeed() == null) ? -1 : servo.getSpeed();
      int pin = getAddress(servo.getPin());
      log.info("================ re-attaching {} {} {} ================", servo.getName(), dm.getId(), pin);
      msg.servoAttach(dm.getId(), pin, uS, (int) speed, servo.getName()); // 
      if (servo.isEnabled()) {
        msg.servoAttachPin(dm.getId(), pin);
      }
    }
  }

  public void connect(String port) {
    connect(port, Serial.BAUD_115200, 8, 1, 0);
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

  public VirtualArduino getVirtual() {
    return virtual;
  }

  /**
   * default params to connect to Arduino &amp; MrlComm.ino FIXME - remove the
   * parameters except rate as they are not allowed to change with MRLComm
   */
  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) {

    // test to see if we've been started. the serial might be null
    
    try {
      
      initSerial();

      if (isConnected() && port.equals(serial.getPortName())) {
        log.info("already connected to port {}", port);
        return;
      }

      if (isVirtual()) {
        if (virtual == null) {
          virtual = (VirtualArduino) Runtime.start("v" + getName(), "VirtualArduino");
        }
        virtual.connect(port);
      }

      serial.connect(port, rate, databits, stopbits, parity);

      // most likely on a real board this send will never get to
      // mrlcomm - because the board is not ready - but it doesnt hurt
      // and in fact it helps VirtualArduino - since we currently do not
      // have a DTR CDR line in the virtual port as use this as a signal
      // of
      // connection

      // by default ack'ing is now on..
      // but with this first msg there is no msg before it,
      // and there is a high probability that the board is not really
      // ready
      // and this msg along with the ack will be ignored
      // so we turn off ack'ing locally

      // TODO - can we re-enable acks ?
      msg.enableAcks(true);
      long startBoardRequestTs = System.currentTimeMillis();

      // start the heartbeat
      enableBoardInfo(boardInfoEnabled);

      log.info("waiting for boardInfo ..........");

      // long waitTime = System.currentTimeMillis();

      // while ts < startedRequest && < 4.5 sec wait 30 try again
      // if timeout report error

      while ((boardInfo == null || boardInfo.getReceiveTs() < startBoardRequestTs) && System.currentTimeMillis() - startBoardRequestTs < 4500) {
        sleep(30);
      }

      log.info("waited {} ms for Arduino {} to say hello", System.currentTimeMillis() - startBoardRequestTs, getName());

      // we might be connected now
      // see what our version is like...
      if (boardInfo != null) {
        Integer version = boardInfo.getVersion();

        if (version == null) {
          error("%s did not get response from arduino....", serial.getPortName());
        } else if (!version.equals(MRLCOMM_VERSION)) {
          error("MrlComm.ino responded with version %s expected version is %s", version, MRLCOMM_VERSION);
        } else {
          info("%s connected on %s responded version %s ... goodtimes...", serial.getName(), serial.getPortName(), version);
        }
      } else {
        log.error("board info is null ! - has MrlComm.ino been loaded ?");
      }

    } catch (Exception e) {
      log.error("serial open threw", e);
      error(e.getMessage());
    }
    
    broadcastState();

  }

  /**
   * sync our device list with mrlcomm
   */
  public void sync() {
    long now = System.currentTimeMillis();
    if (now - syncStartTypeTs < 5000) {
      log.error("===== we are in the middle of synching ... ==== talk to us in {} ms", 5000 - (now - syncStartTypeTs));
      return;
    }
    syncStartTypeTs = System.currentTimeMillis();
    log.warn("================================ sync !!! ==============================");
    try {
      
      for (DeviceMapping device : deviceList.values()) {
        reattach(device);
      }

      List<PinDefinition> list = getPinList();
      for (PinDefinition pindef : list) {
        if (pindef.isEnabled()) {
          enablePin(pindef.getPinName());
        }
      }

    } catch (Exception e) {
      log.error("sync threw", e);
    }
  }

  // > customMsg/[] msg
  public void customMsg(int... params) {
    msg.customMsg(params);
  }

  // @Override
  // > deviceDetach/deviceId
  public void detach(Attachable device) {
    log.info("{} detaching {}", getName(), device.getName());
    // if this service doesn't think its attached, we are done
    if (!isAttached(device)) {
      log.info("device {} not attached", device.getName());
      return;
    }

    // Servo requirements
    if (device instanceof ServoControl && device.isAttached(this)) {
      // if the other service thinks its attached - give it a chance to detach
      // this is important for Servo - because servo will want to disable()
      // before
      // detaching - and it needs the controller to do so...
      device.detach(this);
    }

    log.info("detaching device {}", device.getName());
    Integer id = getDeviceId(device);
    if (id != null) {
      msg.deviceDetach(id);
      deviceIndex.remove(id);
    }
    deviceList.remove(device.getName());
  }

  @Override
  public void detach(String controllerName) {
    detach(Runtime.getService(controllerName));
  }

  @Override
  public void detachI2CControl(I2CControl control) {
    // This method should delete the i2c device entry from the list of
    // I2CDevices
    // The order of the detach is important because the higher level service may
    // want to execute something that
    // needs this service to still be availabe
    if (i2cDevices.containsKey(control.getName())) {
      i2cDevices.remove(control.getName());
      control.detachI2CController(this);
    }

  }

  public void detachI2CControls() {
    for (Map.Entry<String, I2CDeviceMap> i2cDevice : i2cDevices.entrySet()) {
      I2CControl i2cControl = i2cDevice.getValue().control;
      i2cControl.detach(this);
    }
  }

  /**
   * silly Arduino implementation - but keeping it since its familiar
   * digitalWrite/pin/value
   */
  public void digitalWrite(int address, int value) {
    log.info("digitalWrite {} {}", address, value);
    msg.digitalWrite(address, value);
  }

  public void digitalWrite(String pin, int value) {
    PinDefinition pinDef = getPin(pin);
    digitalWrite(pinDef.getAddress(), value);
  }

  @Override
  public Integer getAddress(String pin) {
    PinDefinition pinDef = getPin(pin);
    if (pinDef != null) {
      return pinDef.getAddress();
    }
    try {
      return Integer.parseInt(pin);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * disablePin/address
   */
  @Override
  public void disablePin(int address) {
    PinDefinition pinDef = getPin(address);
    pinDef.setEnabled(false);
    msg.disablePin(address);
  }

  /**
   * disablePin/address
   */
  @Override
  public void disablePin(String pinName) {
    // PinDefinition pinDef = getPin(address);
    PinDefinition pinDef = getPin(pinName);
    pinDef.setEnabled(false);
    msg.disablePin(pinDef.getAddress());
  }

  /**
   * disable all pins
   */
  public void disablePins() {
    msg.disablePins();
  }

  public void disconnect() {
    // FIXED - all don in 'onDisconnect()'
    // enableBoardInfo(false);
    // boardInfo is not valid after disconnect
    // because we might be connecting to a different Arduino
    // boardInfo.reset();
    for (Arduino controller : attachedController.values()) {
      controller.disconnect();
    }
    attachedController.clear();
    if (controllerAttachAs != MRL_IO_NOT_DEFINED) {
      controllerAttachAs = MRL_IO_NOT_DEFINED;
      serial = (Serial) createPeer("serial");
    } else {
      if (serial != null) {
        serial.disconnect();
      }
    }
    broadcastState();
  }

  public void echo(float myFloat, int myByte, float secondFloat) {
    msg.echo(myFloat, myByte, secondFloat);
  }

  // > enableAck/bool enabled
  public void enableAck(boolean enabled) {
    msg.enableAcks(enabled);
  }

  transient BoardInfoPoller poller = new BoardInfoPoller();

  public class BoardInfoPoller implements Runnable {
    boolean running = false;
    Thread thread = null;

    public void run() {
      try {
        running = true;
        while (running) {
          sendBoardInfoRequest();
          sleep(1000);
        }
      } catch (Exception e) {
        log.info("board info stopping {}", e.getMessage());
      }
      thread = null;
      running = false;
    }

    public void start() {
      if (thread == null) {
        thread = new Thread(this, "boardInfoPoller");
        thread.start();
      }
    }

    public void stop() {
      if (thread != null) {
        thread.interrupt();
      }
    }
  }

  // TODO - remove
  // MrlComm now constantantly sends a stream of BoardInfo
  // > enableBoardInfo/bool enabled - no point to this
  public void enableBoardInfo(Boolean enabled) {
    /*
     * if (enabled) { poller.start(); } else { poller.stop(); }
     */
    boardInfoEnabled = enabled;
  }

  @Override
  public void enablePin(int address) {
    enablePin(address, 0);
  }

  // > enablePin/address/type/b16 rate
  public void enablePin(int address, int rate) {
    PinDefinition pinDef = getPin(address);
    msg.enablePin(address, getMrlPinType(pinDef), rate);
    pinDef.setEnabled(true);
    invoke("publishPinDefinition", pinDef); // broadcast pin change
  }

  /**
   * start polling reads of selected pin enablePin/address/type/b16 rate
   */
  public void enablePin(String pin, int rate) {
    if (!isConnected()) {
      error("must be connected to enable pins");
      return;
    }

    PinDefinition pinDef = getPin(pin);
    enablePin(pinDef.getAddress(), rate);
  }

  public String getArduinoPath() {
    return arduinoPath;
  }

  public String getAref() {
    return aref;
  }

  @Override
  public Set<String> getAttached() {
    return deviceList.keySet();
  }

  public int getAttachedCount() {
    return deviceList.size();
  }

  /**
   * Heart-beat method on time, driven by the Arduino service to get information
   * from the board its currently connected. This is the "last" boardInfo
   * returned from the task inserted with addTask("getBoardInfo", 1000, 0,
   * "sendBoardInfoRequest");
   * 
   * getBoardInfo
   */
  public BoardInfo getBoardInfo() {
    return boardInfo;
  }

  @Override // override to get Arduino board types
  public List<BoardType> getBoardTypes() {

    List<BoardType> boardTypes = new ArrayList<BoardType>();
    try {
      String b = FileIO.resourceToString("Arduino" + File.separator + "boards.txt");
      Properties boardProps = new Properties();
      boardProps.load(new ByteArrayInputStream(b.getBytes()));

      Enumeration<?> e = boardProps.propertyNames();
      Set<String> distinct = new TreeSet<String>();
      Set<String> hasProcessorTypes = new TreeSet<String>();
      while (e.hasMoreElements()) {
        String keyLine = (String) e.nextElement();
        String[] parts = keyLine.split("\\.");
        String key = parts[0];
        if (key.startsWith("menu")) {
          continue;
        }

        if (keyLine.contains("menu.cpu")) {
          hasProcessorTypes.add(key);
          // split - remove previous
          if (distinct.contains(key)) {
            distinct.remove(key);
          }
          // for diecimila.atmega328
          try {
            key = parts[0] + "." + parts[3];
          } catch (Exception e2) {
            log.error("board.txt is weird", e2);
          }
          distinct.add(key);
        } else if (!hasProcessorTypes.contains(key)) {
          distinct.add(key);
        }
      }

      for (String longKey : distinct) {

        String[] parts = longKey.split("\\.");
        String key = parts[0];
        String processorType = null;
        if (parts.length > 1) {
          processorType = parts[1];
        }

        BoardType boardType = new BoardType();

        if (processorType != null) {
          boardType.setName(boardProps.getProperty(String.format("%s.name", key)) + " - " + processorType);
        } else {
          boardType.setName(boardProps.getProperty(String.format("%s.name", key)));
        }

        boardType.setBoard(longKey);
        boardType.setId(longKey.hashCode());
        boardTypes.add(boardType);
      }
    } catch (Exception e) {
      log.error("getBoards threw", e);
    }
    return boardTypes;
  }

  @Override
  public org.myrobotlab.math.interfaces.Mapper getDefaultMapper() {
    // best guess :P
    MapperLinear mapper = new MapperLinear();
    mapper.map(-1.0, 1.0, 0.0, 255.0);
    return mapper;
  }

  public Attachable getDevice(Integer deviceId) {
    DeviceMapping dm = deviceIndex.get(deviceId);
    if (dm == null) {
      log.error("no device with deviceId {}", deviceId);
      return null;
    }
    return dm.getDevice();
  }

  Integer getDeviceId(NameProvider device) {
    return getDeviceId(device.getName());
  }

  Integer getDeviceId(String name) {
    if (deviceList.containsKey(name)) {
      Integer id = deviceList.get(name).getId();
      if (id == null) {
        error("cannot get device id for %s - device attempetd to attach - but I suspect something went wrong", name);
      }
      return id;
    }
    log.error("getDeviceId could not find device {}", name);
    return null;
  }

  private String getDeviceName(int deviceId) {
    if (getDevice(deviceId) == null) {
      log.error("getDeviceName({}) is null", deviceId);
      return null;
    }
    return getDevice(deviceId).getName();
  }

  /**
   * int type to describe the pin defintion to Pin.h 0 digital 1 analog
   * 
   */
  public Integer getMrlPinType(PinDefinition pin) {
    if (board == null) {
      error("must have pin board type to determin pin definition");
      return null;
    }

    if (pin == null) {
      log.error("pin definition null");
      return null;
    }

    if (pin.isAnalog()) {
      return 1;
    }

    return 0;
  }

  /**
   * FIXME - have local This creates the pin definitions based on boardType Not
   * sure how many pin definition sets there are. Currently there are only 2
   * supported - Mega-Like 70 pins &amp; Uno-Like 20 pins (14 digital 6 analog)
   * FIXME - sync with VirtualArduino FIXME - String boardType
   */
  @Override // override for arduino to get pin list
  public List<PinDefinition> getPinList() {
    // 2 board types have been identified (perhaps this is based on
    // processor?)
    // mega-like & uno like

    // if no change - just return the values
    if ((pinIndex != null && board.contains("mega") && pinIndex.size() == 70) || (pinIndex != null && !board.contains("mega") && pinIndex.size() == 20)) {
      return new ArrayList<PinDefinition>(pinIndex.values());
    }

    // create 2 indexes for fast retrieval
    // based on "name" or "address"
    pinMap.clear();
    pinIndex.clear();

    List<PinDefinition> pinList = new ArrayList<PinDefinition>();

    if (board.contains("mega")) {
      for (int i = 0; i < 70; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);
        // begin wacky pin def logic
        String pinName = null;
        if (i == 0) {
          pindef.setRx(true);
        }
        if (i == 1) {
          pindef.setTx(true);
        }
        if (i < 1 || (i > 13 && i < 54)) {
          pinName = String.format("D%d", i);
          pindef.setDigital(true);
        } else if (i > 53) {
          pinName = String.format("A%d", i - 54);
          pindef.setAnalog(true);
          pindef.setDigital(true);
          pindef.canWrite(true);
        } else {
          pinName = String.format("D%d", i);
          pindef.setPwm(true);
        }
        pindef.setPinName(pinName);
        pindef.setAddress(i);
        pinMap.put(pinName, pindef);
        pinIndex.put(pindef.getAddress(), pindef);
        pinList.add(pindef);
      }
    } else {
      for (int i = 0; i < 20; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);
        String pinName = null;
        if (i == 0) {
          pindef.setRx(true);
        }
        if (i == 1) {
          pindef.setTx(true);
        }
        if (i < 14) {
          pinName = String.format("D%d", i);
          pindef.setDigital(true);
        } else {
          pindef.setAnalog(true);
          pindef.canWrite(false);
          pindef.setDigital(false);
          pinName = String.format("A%d", i - 14);
        }
        if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
          pindef.setPwm(true);
          pinName = String.format("D%d", i);
        }
        pindef.setPinName(pinName);
        pindef.setAddress(i);
        pinMap.put(pinName, pindef);
        pinIndex.put(pindef.getAddress(), pindef);
        pinList.add(pindef);
      }

      // FIXME - nano pico other ???
      if (board.contains("nano")) {
        /*
         * int i = 20; pinName = String.format("A%d", i - 14); PinDefinition
         * pindef = new PinDefinition(getName(), i); pindef.setDigital(false);
         * pindef.setPwm(false); pindef.setAnalog(true); pindef.canWrite(false);
         * pinIndex.put(i, pindef); pinMap.put(pinName, pindef);
         */
      }
    }
    return pinList;
  }

  public String getPortName() {
    return serial.getPortName();
  }

  @Override
  public List<String> getPortNames() {
    if (serial != null) {
      return serial.getPortNames();
    }
    return new ArrayList<String>();
  }

  @Override
  public List<String> getPorts() {
    // we use pins not ports
    List<String> ret = new ArrayList<String>();
    return ret;
  }

  /*
   * Use the serial service for serial activities ! No reason to replicate
   * methods
   */
  public Serial getSerial() {
    return serial;
  }

  public Sketch getSketch() {
    return sketch;
  }

  /**
   * Internal Arduino method to create an i2cBus object in MrlComm that is
   * shared between all i2c devices
   * 
   * @param control
   * @param busAddress
   */
  // > i2cBusAttach/deviceId/i2cBus
  private void i2cBusAttach(I2CBusControl control, int busAddress) {
    DeviceMapping dm = attachDevice(i2cBus, new Object[] { busAddress });
    Integer deviceId = dm.getId();
    msg.i2cBusAttach(deviceId, busAddress);
  }

  @Override
  // > i2cRead/deviceId/deviceAddress/size
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    i2cDataReturned = false;
    // Get the device index to the MRL i2c bus
    String i2cBus = String.format("I2CBus%s", busAddress);
    int deviceId = getDeviceId(i2cBus);
    log.info("i2cRead requesting {} bytes", size);
    msg.i2cRead(deviceId, deviceAddress, size);

    int retry = 0;
    int retryMax = 1000; // ( About 1000ms = s)
    try {
      /**
       * We will wait up to retryMax times to get the i2c data back from
       * MrlComm.c and wait 1 ms between each try. A blocking queue is not
       * needed, as this is only a single data element - and blocking is not
       * necessary.
       */
      while ((retry < retryMax) && (!i2cDataReturned)) {
        sleep(1);
        ++retry;
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    if (i2cDataReturned) {
      log.debug("i2cReturnData returned {} bytes to caller {}.", i2cDataSize, control.getName());
      for (int i = 0; i < i2cDataSize; i++) {
        buffer[i] = i2cData[i];
        log.debug("i2cReturnData returned ix {} value {}", i, buffer[i]);
      }
      return i2cDataSize;
    }
    // Time out, no data returned
    return -1;
  }

  /**
   * This methods is called by the i2cBus object when data is returned from the
   * i2cRead It populates the i2cData area and sets the i2cDataReturned flag to
   * true so that the loop in i2cRead can return the data to the caller
   * 
   */
  @Override
  public void i2cReturnData(int[] rawData) {
    i2cDataSize = rawData.length;
    for (int i = 0; i < i2cDataSize; i++) {
      i2cData[i] = (byte) (rawData[i] & 0xff);
    }
    log.debug("i2cReturnData invoked. i2cDataSize = {}", i2cDataSize);
    i2cDataReturned = true;
  }

  @Override
  // > i2cWrite/deviceId/deviceAddress/[] data
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    String i2cBus = String.format("I2CBus%s", busAddress);
    int deviceId = getDeviceId(i2cBus);

    int data[] = new int[size];
    for (int i = 0; i < size; ++i) {
      data[i] = buffer[i];// guess you want -128 to 127 ?? [ ] == unsigned
      // char & 0xff;
    }

    msg.i2cWrite(deviceId, deviceAddress, data);
  }

  @Override
  // > i2cWriteRead/deviceId/deviceAddress/readSize/writeValue
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
    if (writeSize != 1) {
      i2cWrite(control, busAddress, deviceAddress, writeBuffer, writeSize);
      return i2cRead(control, busAddress, deviceAddress, readBuffer, readSize);
    } else {
      i2cDataReturned = false;
      // Get the device index to the MRL i2c bus
      String i2cBus = String.format("I2CBus%s", busAddress);
      int deviceId = getDeviceId(i2cBus);

      int msgBuffer[] = new int[4];
      msgBuffer[0] = deviceId;
      msgBuffer[1] = deviceAddress;
      msgBuffer[2] = readSize;
      msgBuffer[3] = writeBuffer[0];
      msg.i2cWriteRead(deviceId, deviceAddress, readSize, writeBuffer[0] & 0xFF);
      int retry = 0;
      int retryMax = 1000; // ( About 1000ms = s)
      try {
        /**
         * We will wait up to retryMax times to get the i2c data back from
         * MrlComm.c and wait 1 ms between each try. A blocking queue is not
         * needed, as this is only a single data element - and blocking is not
         * necessary.
         */
        while ((retry < retryMax) && (!i2cDataReturned)) {
          sleep(1);
          ++retry;
        }
      } catch (Exception e) {
        Logging.logError(e);
      }
      if (i2cDataReturned) {
        log.debug("i2cReturnData returned %s bytes to caller {}.", i2cDataSize, control.getName());
        for (int i = 0; i < i2cDataSize; i++) {
          readBuffer[i] = i2cData[i];
          log.debug("i2cReturnData returned ix {} value {}", i, readBuffer[i]);
        }
        return i2cDataSize;
      }
      // Time out, no data returned
      return -1;
    }
  }

  private void initSerial() {
    if (msg == null) {
      serial = (Serial) startPeer("serial");
      msg = new Msg(this, serial);
      serial.addByteListener(this);
    }
  }

  @Override
  public boolean isAttached(Attachable device) {
    return deviceList.containsKey(device.getName());
  }

  @Override
  public boolean isAttached(String name) {
    return deviceList.containsKey(name);
  }

  @Override
  public boolean isConnected() {
    // include that we must have gotten a valid MrlComm version number.
    if (serial != null && serial.isConnected() && boardInfo != null && boardInfo.getVersion() != null) {
      return true;
    }
    // just to force serial arduino conected if it is a serialX com
    // usefull to enable pin on the remote arduino
    // @Deprecated FIXME - this is "bad"
    if ((controllerAttachAs == MRL_IO_SERIAL_1 || controllerAttachAs == MRL_IO_SERIAL_2 || controllerAttachAs == MRL_IO_SERIAL_3) && boardInfo.getVersion() == MRLCOMM_VERSION) {
      return true;
    }
    return false;
  }

  // FIXME put recording in generated message structure !!!
  @Override
  public boolean isRecording() {
    return msg.isRecording();
  }

  // not used currently - should be refactored to use these methods for motor
  // control
  @Override
  public double motorCalcOutput(MotorControl mc) {
    double value = mc.calcControllerOutput();
    return value;
  }

  @Override
  public void motorMove(MotorControl mc) {

    Class<?> type = mc.getClass();

    double powerOutput = motorPowerMapper.calcOutput(mc.getPowerLevel());
    // log.info(mc.getPowerLevel()+" "+powerOutput);

    if (Motor.class == type) {
      Motor config = (Motor) mc;
      msg.digitalWrite(getAddress(config.getDirPin()), (powerOutput < 0) ? MOTOR_BACKWARD : MOTOR_FORWARD);
      msg.analogWrite(getAddress(config.getPwrPin()), (int) Math.abs(powerOutput));
    } else if (MotorDualPwm.class == type) {
      MotorDualPwm config = (MotorDualPwm) mc;
      if (powerOutput < 0) {
        msg.analogWrite(getAddress(config.getLeftPwmPin()), 0);
        msg.analogWrite(getAddress(config.getRightPwmPin()), (int) Math.abs(powerOutput));
      } else if (powerOutput > 0) {
        msg.analogWrite(getAddress(config.getRightPwmPin()), 0);
        msg.analogWrite(getAddress(config.getLeftPwmPin()), (int) Math.abs(powerOutput));
      } else {
        msg.analogWrite(getAddress(config.getLeftPwmPin()), 0);
        msg.analogWrite(getAddress(config.getRightPwmPin()), 0);
      }
    } else {
      error("motorMove for motor type %s not supported", type);
    }

  }

  // FIXME - clean or remove ...
  // ========== pulsePin begin =============
  // FIXME - MasterBlaster had a pulse motor which could support MoveTo
  // We need a Motor + encoder (analog or digital) DiyServo does this...
  @Override
  public void motorMoveTo(MotorControl mc) {
    // speed parameter?
    // modulo - if < 1
    // speed = 1 else
    log.info("motorMoveTo targetPos {} powerLevel {}", mc.getTargetPos(), mc.getPowerLevel());

    Class<?> type = mc.getClass();

    // if pulser (with or without fake encoder
    // send a series of pulses !
    // with current direction
    if (Motor.class == type) {
      Motor motor = (Motor) mc;
      // check motor direction
      // send motor direction
      // TODO powerLevel = 100 * powerlevel

      // FIXME !!! - this will have to send a Long for targetPos at some
      // point !!!!
      double target = Math.abs(motor.getTargetPos());

      int b0 = (int) target & 0xff;
      int b1 = ((int) target >> 8) & 0xff;
      int b2 = ((int) target >> 16) & 0xff;
      int b3 = ((int) target >> 24) & 0xff;

      // TODO FIXME
      // sendMsg(PULSE, deviceList.get(motor.getName()).id, b3, b2, b1,
      // b0, (int) motor.getPowerLevel(), feedbackRate);
    }

  }

  @Override
  public void motorReset(MotorControl motor) {
    // perhaps this should be in the motor control
    // motor.reset();
    // opportunity to reset variables on the controller
    // sendMsg(MOTOR_RESET, motor.getind);
  }

  @Override
  public void motorStop(MotorControl mc) {

    Class<?> type = mc.getClass();

    if (Motor.class == type) {
      Motor config = (Motor) mc;
      msg.analogWrite(getAddress(config.getPwrPin()), 0);
    } else if (MotorDualPwm.class == type) {
      MotorDualPwm config = (MotorDualPwm) mc;
      msg.analogWrite(getAddress(config.getLeftPwmPin()), 0);
      msg.analogWrite(getAddress(config.getRightPwmPin()), 0);
    }
  }

  @Override
  // > neoPixelAttach/deviceId/pin/b32 numPixels
  public void neoPixelAttach(NeoPixel neopixel, int pin, int numPixels) {
    DeviceMapping dm = attachDevice(neopixel, new Object[] { pin, numPixels });
    Integer deviceId = dm.getId();
    msg.neoPixelAttach(getDeviceId(neopixel)/* byte */, pin/* byte */,
        numPixels/* b32 */);
  }

  @Override
  // > neoPixelSetAnimation/deviceId/animation/red/green/blue/b16 speed
  public void neoPixelSetAnimation(NeoPixel neopixel, int animation, int red, int green, int blue, int speed) {
    msg.neoPixelSetAnimation(getDeviceId(neopixel), animation, red, green, blue, speed);
  }

  /**
   * neoPixelWriteMatrix/deviceId/[] buffer
   */
  @Override
  public void neoPixelWriteMatrix(NeoPixel neopixel, List<Integer> data) {
    int[] buffer = new int[data.size()];
    for (int i = 0; i < data.size(); ++i) {
      buffer[i] = data.get(i);
    }
    msg.neoPixelWriteMatrix(getDeviceId(neopixel), buffer);
  }

  /**
   * Callback for Serial service - local (not remote) although a
   * publish/subscribe could be created - this method is called by a thread
   * waiting on the Serial's RX BlockingQueue
   *
   * Other services may use the same technique or subscribe to a Serial's
   * publishByte method
   *
   * it might be worthwhile to look in optimizing reads into arrays vs single
   * byte processing .. but maybe there would be no gain
   *
   */

  // FIXME - onByte(int[] data)
  @Override
  public Integer onByte(Integer newByte) {
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

      ++byteCount;
      if (log.isDebugEnabled()) {
        log.info("onByte {} \tbyteCount \t{}", newByte, byteCount);
      }
      if (byteCount == 1) {
        if (newByte != MAGIC_NUMBER) {
          byteCount = 0;
          msgSize = 0;
          Arrays.fill(ioCmd, 0); // FIXME - optimize - remove
          warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++errorServiceToHardwareRxCnt));
          // dump.setLength(0);
        }
        return newByte;
      } else if (byteCount == 2) {
        // get the size of message
        if (newByte > 64) {
          byteCount = 0;
          msgSize = 0;
          error(String.format("Arduino->MRL error %d rx sz errors", ++errorServiceToHardwareRxCnt));
          return newByte;
        }
        msgSize = newByte.intValue();
        // dump.append(String.format("MSG|SZ %d", msgSize));
      } else if (byteCount > 2) {
        // remove header - fill msg data - (2) headbytes -1
        // (offset)
        // dump.append(String.format("|P%d %d", byteCount,
        // newByte));
        ioCmd[byteCount - 3] = newByte.intValue();
      } else {
        // the case where byteCount is negative?! not got.
        error(String.format("Arduino->MRL error %d rx negsz errors", ++errorServiceToHardwareRxCnt));
        return newByte;
      }
      if (byteCount == 2 + msgSize) {
        // we've received a full message

        msg.processCommand(ioCmd);

        // Our 'first' getBoardInfo may not receive a acknowledgement
        // so this should be disabled until boadInfo is valid

        // clean up memory/buffers
        msgSize = 0;
        byteCount = 0;
        Arrays.fill(ioCmd, 0); // optimize remove
      }
    } catch (Exception e) {
      ++errorHardwareToServiceRxCnt;
      error("msg structure violation %d", errorHardwareToServiceRxCnt);
      log.warn("msg_structure violation byteCount {} buffer {}", byteCount, Arrays.copyOf(ioCmd, byteCount));
      // try again (clean up memory buffer)
      msgSize = 0;
      byteCount = 0;
      Logging.logError(e);
    }
    return newByte;
  }

  @Override
  public void onConnect(String portName) {
    info("%s connected to %s", getName(), portName);
    // chained...
    invoke("publishConnect", portName);
  }

  public void onCustomMsg(Integer ax, Integer ay, Integer az) {
    log.info("onCustomMsg");
  }

  @Override
  public void onDisconnect(String portName) {
    info("%s disconnected from %s", getName(), portName);
    // enableAck(false);
    enableBoardInfo(false);
    // chained...
    invoke("publishDisconnect", portName);
  }

  public void openMrlComm(String path) {
    try {

      if (!setArduinoPath(path)) {
        return;
      }

      String mrlCommFiles = null;
      if (FileIO.isJar()) {
        mrlCommFiles = Util.getResourceDir() + "/Arduino/MrlComm";
        // FIXME - don't do this every time :P
        Zip.extractFromSelf(Util.getResourceDir() + File.separator + "Arduino" + File.separator + "MrlComm", "resource/Arduino/MrlComm");
      } else {
        // running in IDE ?
        mrlCommFiles = Util.getResourceDir() + File.separator + "Arduino" + File.separator + "MrlComm";
      }
      File mrlCommDir = new File(mrlCommFiles);
      if (!mrlCommDir.exists() || !mrlCommDir.isDirectory()) {
        error("mrlcomm script directory %s is not a valid", mrlCommDir);
        return;
      }
      String exePath = arduinoPath + File.separator + ArduinoUtils.getExeName();
      String inoPath = mrlCommDir.getAbsolutePath() + File.separator + "/MrlComm.ino";
      List<String> cmd = new ArrayList<String>();
      cmd.add(exePath);
      cmd.add(inoPath);
      ProcessBuilder builder = new ProcessBuilder(cmd);
      builder.start();

    } catch (Exception e) {
      error(String.format("%s %s", e.getClass().getSimpleName(), e.getMessage()));
      log.error("openMrlComm threw", e);
    }
  }
  
  public String getBase64ZippedMrlComm() {
    return Base64.getEncoder().encodeToString((getZippedMrlComm()));
  }

  public byte[] getZippedMrlComm() {
    try {
      // get resource location
      String filename = getDataDir() + File.separator + "MrlComm.zip";
      File f = new File(filename);
      if (f.exists()) {
        f.delete();
      }

      // zip resource
      Zip.zip(new String[] { getResourceDir() + File.separator + "MrlComm" }, filename);

      // return zip file
      return FileIO.toByteArray(new File(filename));
    } catch (Exception e) {
      error("could not get zipped mrl comm %s", e);
    }
    return null;
  }

  @Override
  /**
   * // > pinMode/pin/mode
   */
  public void pinMode(int address, String modeStr) {
    pinMode(address, modeStr.equalsIgnoreCase("INPUT") ? Arduino.INPUT : Arduino.OUTPUT);
  }

  public void pinMode(int address, int mode) {
    msg.pinMode(address, mode);
  }

  /**
   * With Arduino we want to be able to do pinMode("D7", "INPUT"), but it should
   * not be part of the PinArrayControl interface - because when it comes down
   * to it .. a pin MUST ALWAYS have an address regardless what you label or
   * name it...
   * 
   */
  public void pinMode(String pin, String mode) {
    PinDefinition pinDef = getPin(pin);
    pinMode(pinDef.getAddress(), mode);
  }

  // < publishAck/function
  public void publishAck(Integer function/* byte */) {
    log.debug("Message Ack received: =={}==", Msg.methodToString(function));

    msg.ackReceived(function);

    numAck++;
  }

  // < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/[]
  // deviceSummary
  public BoardInfo publishBoardInfo(Integer version/* byte */,
      Integer boardTypeId/* byte */, Integer microsPerLoop/* b16 */,
      Integer sram/* b16 */, Integer activePins, int[] deviceSummary/* [] */) {

    String boardTypeName = getBoardType(boardTypeId);

    boardInfo = new BoardInfo(version, boardTypeId, boardTypeName, microsPerLoop, sram, activePins, arrayToDeviceSummary(deviceSummary), boardInfoRequestTs);

    boardInfoRequestTs = System.currentTimeMillis();

    log.debug("Version return by Arduino: {}", boardInfo.getVersion());
    log.debug("Board type currently set: {} => {}", boardTypeId, boardTypeName);

    if (lastBoardInfo == null || !lastBoardInfo.getBoardTypeName().equals(board)) {
      log.warn("setting board to type {}", board);
      this.board = boardInfo.getBoardTypeName();
      // we don't invoke, because
      // it might get into a race condition
      // in some gui
      getPinList();
      // invoke("getPinList");
      broadcastState();
    }

    if (boardInfo != null) {
      DeviceSummary[] ds = boardInfo.getDeviceSummary();
      if (deviceList.size() - 1 > ds.length) { /* -1 for self */
        sync();
      }
    }

    // we send here - because this is a "command" message, and we don't want the
    // possibility of
    // block this "status" msgs
    // send(getName(),"sync", boardInfo);
    lastBoardInfo = boardInfo;
    return boardInfo;
  }

  @Override
  public String publishConnect(String portName) {
    return portName;
  }

  // < publishCustomMsg/[] msg
  public int[] publishCustomMsg(int[] msg/* [] */) {
    return msg;
  }

  // < publishDebug/str debugMsg
  public String publishDebug(String debugMsg/* str */) {
    log.info("publishDebug {}", debugMsg);
    return debugMsg;
  }

  @Override
  public String publishDisconnect(String portName) {
    return portName;
  }

  /**
   * publishEcho/b32 sInt/str name1/b8/bu32 bui32/b32 bi32/b9/str name2/[]
   * 
   * @param myFloat
   * @param myByte
   * @param secondFloat
   */
  public void publishEcho(float myFloat, int myByte, float secondFloat) {
    log.info("myFloat {} {} {} ", myFloat, myByte, secondFloat);
  }

  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    return data;
  }

  // callback for generated method from arduinoMsg.schema
  public EncoderData publishEncoderData(Integer deviceId, Integer position) {
    EncoderControl ec = (EncoderControl) getDevice(deviceId);
    String pin = null;
    if (ec instanceof Amt203Encoder) {
      // type = 0;
      pin = ((Amt203Encoder) ec).getPin();
    } else if (ec instanceof As5048AEncoder) {
      // type = 1;
      pin = ((As5048AEncoder) ec).getPin();
    } else {
      error("unknown encoder type {}", ec.getClass().getName());
    }

    EncoderData data = new EncoderData(ec.getName(), pin, position);
    return data;
  }

  /*
   * DeviceControl methods. In this case they represents the I2CBusControl Not
   * sure if this is good to use the Arduino as an I2CBusControl Exploring
   * different alternatives. I may have to rethink. Alternate solutions are
   * welcome. /Mats.
   */

  /**
   * @param deviceId
   *          - mrl device identifier
   * @param data
   *          - data to publish from I2c
   */
  // < publishI2cData/deviceId/[] data
  public void publishI2cData(Integer deviceId, int[] data) {
    log.info("publishI2cData");
    i2cReturnData(data);
  }

  /**
   * error from mrlcom in string form
   * 
   * @param errorMsg
   * @return
   */
  // < publishMRLCommError/str errorMsg
  public String publishMRLCommError(String errorMsg/* str */) {
    log.error(errorMsg);
    return errorMsg;
  }

  // < publishPinArray/[] data
  public PinData[] publishPinArray(int[] data) {
    log.debug("publishPinArray {}", data);
    // if subscribers -
    // look for subscribed pins and publish them

    int pinDataCnt = data.length / 3;
    PinData[] pinArray = new PinData[pinDataCnt];

    // parse sort reduce ...
    for (int i = 0; i < pinArray.length; ++i) {
      int address = data[3 * i];
      PinDefinition pinDef = getPin(address);
      if (pinDef == null) {
        log.error("not a valid pin address {}", address);
        continue;
      }
      int value = Serial.bytesToInt(data, (3 * i) + 1, 2);
      PinData pinData = new PinData(pinDef.getPinName(), value);
      // update def with last value
      pinDef.setValue(value);
      pinArray[i] = pinData;

      // handle individual pins
      if (pinListeners.containsKey(address)) {
        Set<PinListener> set = pinListeners.get(address);
        for (PinListener pinListner : set) {
          if (pinListner.isLocal()) {
            pinListner.onPin(pinData);
          } else {
            invoke("publishPin", pinData);
          }
        }
      }
    }

    // TODO: improve this logic so it doesn't something more effecient.
    HashMap<String, PinData> pinDataMap = new HashMap<String, PinData>();
    for (int i = 0; i < pinArray.length; i++) {
      if (pinArray[i] != null && pinArray[i].pin != null) {
        pinDataMap.put(pinArray[i].pin, pinArray[i]);
      }
    }

    for (String name : pinArrayListeners.keySet()) {
      // put the pin data into a map for quick lookup
      PinArrayListener pal = pinArrayListeners.get(name);
      if (pal.getActivePins() != null && pal.getActivePins().length > 0) {
        int numActive = pal.getActivePins().length;
        PinData[] subArray = new PinData[numActive];
        for (int i = 0; i < numActive; i++) {
          String key = pal.getActivePins()[i];
          if (pinDataMap.containsKey(key)) {
            subArray[i] = pinDataMap.get(key);
          } else {
            subArray[i] = null;
          }
        }
        // only the values that the listener is asking for.
        pal.onPinArray(subArray);
      } else {
        // the full array
        pal.onPinArray(pinArray);
      }
    }
    return pinArray;
  }

  public List<String> publishPortNames(List<String> portNames) {
    return portNames;
  }

  /**
   * FIXME - I bet this doesnt work - test it
   * 
   * @param deviceId
   * @param data
   * @return
   */
  public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
    SerialRelayData serialData = new SerialRelayData(deviceId, data);
    return serialData;
  }

  @Deprecated /**
               * Controllers should publish EncoderData - Servos can change that
               * into ServoData and publish REMOVED BY GROG - use TimeEncoder !
               */
  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
    if (getDevice(deviceId) != null) {
      // REMOVED BY GROG - use time encoder !((ServoControl)
      // getDevice(deviceId)).publishServoData(ServoStatus.SERVO_POSITION_UPDATE,
      // (double) currentPos);
    } else {
      error("no servo found at device id %d", deviceId);
    }
    return currentPos;
  }

  // FIXME should be in Control interface - for callback
  // < publishUltrasonicSensorData/deviceId/b16 echoTime
  public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime) {
    // log.info("echoTime {}", echoTime);
    ((UltrasonicSensor) getDevice(deviceId)).onUltrasonicSensorData(echoTime.doubleValue());
    return echoTime;
  }

  // FIXME put recording into generated Msg
  @Override
  public void record() throws Exception {
    msg.record();
  }

  @Override
  public void releaseService() {
    super.releaseService();
    if (virtual != null) {
      virtual.releaseService();
    }
    sleep(300);
    disconnect();
  }

  /**
   * resets both MrlComm-land &amp; Java-land
   */
  public void reset() {
    log.info("reset - resetting all devices");

    // reset MrlComm-land
    softReset();

    for (String name : deviceList.keySet()) {
      DeviceMapping dmap = deviceList.get(name);
      Attachable device = dmap.getDevice();
      log.info("unsetting device {}", name);
      try {
        device.detach(name);
      } catch (Exception e) {
        log.error("detaching threw", e);
      }
    }

    // reset Java-land
    deviceIndex.clear();
    deviceList.clear();
    errorHardwareToServiceRxCnt = 0;
    errorServiceToHardwareRxCnt = 0;
  }

  /**
   * Requesting board information from the board
   */
  public void sendBoardInfoRequest() {
    boardInfoRequestTs = System.currentTimeMillis();
    msg.getBoardInfo();
  }

  public void serialAttach(SerialRelay serialRelay, int controllerAttachAs) {
    DeviceMapping dm = attachDevice(serialRelay, new Object[] { controllerAttachAs });
    Integer deviceId = dm.getId();
    msg.serialAttach(deviceId, controllerAttachAs);
  }

  // > servoDetachPin/deviceId
  public void onServoDisable(ServoControl servo) {
    msg.servoDetachPin(getDeviceId(servo));
  }

  @Override
  public void onServoEnable(ServoControl servo) {
    Integer deviceId = getDeviceId(servo);
    if (deviceId == null) {
      log.warn("servoEnable servo {} does not have a corresponding device currently - did you attach?", servo.getName());
    }
    if (isConnected()) {
      msg.servoAttachPin(deviceId, getAddress(servo.getPin()));
    } else {
      log.info("not currently connected");
    }
  }

  /**
   * servo.write(angle) https://www.arduino.cc/en/Reference/ServoWrite The msg
   * to mrl will always contain microseconds - but this method will (like the
   * Arduino Servo.write) accept both degrees or microseconds. The code is
   * ported from Arduino's Servo.cpp
   */
  @Override
  // > servoWrite/deviceId/target
  public void onServoMoveTo(ServoControl servo) {
    Integer deviceId = getDeviceId(servo);
    if (deviceId == null) {
      log.warn("servoMoveTo servo {} does not have a corresponding device currently - did you attach?", servo.getName());
      return;
    }
    // getTargetOutput ALWAYS ALWAYS Degrees !
    // so we convert to microseconds
    int us = degreeToMicroseconds(servo.getTargetOutput());
    log.debug("servoMoveToMicroseconds servo {} id {} {}->{} us", servo.getName(), deviceId, servo.getPos(), us);
    msg.servoMoveToMicroseconds(deviceId, us);
  }

  @Override
  // > servoSetVelocity/deviceId/b16 velocity
  public void onServoSetSpeed(ServoControl servo) {
    int speed = -1;
    if (servo.getSpeed() != null) {
      speed = servo.getSpeed().intValue();
    }
    log.info("servoSetVelocity {} id {} velocity {}", servo.getName(), getDeviceId(servo), speed);
    Integer i = getDeviceId(servo);
    if (i == null) {
      log.error("{} has null deviceId", servo);
      return;
    }
    msg.servoSetVelocity(i, speed);
  }


  /**
   * On standard servos a parameter value of 1000 is fully counter-clockwise,
   * 2000 is fully clockwise, and 1500 is in the middle.
   */
  @Override
  // > servoWriteMicroseconds/deviceId/b16 ms
  public void onServoWriteMicroseconds(ServoControl servo, int uS) {
    int deviceId = getDeviceId(servo);
    log.debug("writeMicroseconds {} {} id {}", servo.getName(), uS, deviceId);
    msg.servoMoveToMicroseconds(deviceId, uS);
  }

  public boolean setArduinoPath(String path) {

    path = path.replace("\\", "/");
    path = path.trim();
    if (!path.endsWith("/")) {
      path += "/";
    }

    File dir = new File(path);
    if (!dir.exists() || !dir.isDirectory()) {
      error(String.format("%s is not a valid directory", path));
      return false;
    }
    arduinoPath = path;
    ArduinoUtils.arduinoPath = arduinoPath; // THIS IS SILLY AND NOT
    // NORMALIZED !
    save();
    return true;
  }

  public void setAref(String aref) {
    aref = aref.toUpperCase();
    if (this.getBoard().contains("mega")) {
      if (aref == "INTERNAL") {
        error("Aref " + aref + " is not compatible with your board " + this.getBoard());
        aref = "DEFAULT";
      }
    } else {
      if (aref == "INTERNAL1V1" || aref == "INTERNAL2V56") {
        error("Aref INTERNALxV is not compatible with your board " + this.getBoard());
        aref = "DEFAULT";
      }
    }

    int arefInt = 1;
    switch (aref) {
      case "EXTERNAL":
        arefInt = 0;
        break;
      case "DEFAULT":
        arefInt = 1;
        break;
      case "INTERNAL1V1":
        arefInt = 2;
        break;
      case "INTERNAL":
        arefInt = 3;
        break;
      case "INTERNAL2V56":
        arefInt = 3;
        break;
      default:
        log.error("Aref " + aref + " is unknown");
    }
    log.info("set aref to " + aref);
    this.aref = aref;
    msg.setAref(arefInt);
  }

  public void setBoardMega() {
    setBoard(BOARD_TYPE_MEGA);
  }

  public void setBoardMegaADK() {
    setBoard(BOARD_TYPE_MEGA_ADK);
  }

  public void setBoardNano() {
    setBoard(BOARD_TYPE_NANO);
  }

  public void setBoardUno() {
    setBoard(BOARD_TYPE_UNO);
  }

  /*
   * Debounce ensures that only a single signal will be acted upon for a single
   * opening or closing of a contact. the delay is the min number of pc cycles
   * must occur before a reading is taken
   *
   * Affects all reading of pins setting to 0 sets it off
   *
   * TODO - implement on MrlComm side ...
   * 
   */
  // > setDebounce/pin/delay
  public void setDebounce(int pin, int delay) {
    msg.setDebounce(pin, delay);
  }

  // > setDebug/bool enabled
  public void setDebug(boolean b) {
    msg.setDebug(b);
  }

  /*
   * dynamically change the serial rate TODO - shouldn't this change Arduino
   * service serial rate too to match?
   * 
   */
  // > setSerialRate/b32 rate
  public void setSerialRate(int rate) {
    msg.setSerialRate(rate);
  }

  public void setSketch(Sketch sketch) {
    this.sketch = sketch;
    broadcastState();
  }

  /*
   * set a pin trigger where a value will be sampled and an event will be signal
   * when the pin turns into a different state.
   * 
   * TODO - implement on MrlComm side...
   */
  // > setTrigger/pin/triggerValue
  public void setTrigger(int pin, int value) {
    msg.setTrigger(pin, value);
  }

  @Override
  public void setZeroPoint(EncoderControl encoder) {
    // send the set zero point command to the encoder
    msg.setZeroPoint(getDeviceId(encoder.getName()));
  }

  /**
   * send a reset to MrlComm - all devices removed, all polling is stopped and
   * all other counters are reset
   */
  // > softReset
  public void softReset() {
    msg.softReset();
  }

  @Override
  public void startService() {
    super.startService();
    try {
      initSerial();
    } catch (Exception e) {
      log.error("Arduino.startService threw", e);
    }
  }

  @Override
  public void stopRecording() {
    msg.stopRecording();
  }

  @Override
  public void stopService() {
    super.stopService();
    detachI2CControls();
    disconnect();
  }

  @Override
  // > ultrasonicSensorStartRanging/deviceId/b32 timeout
  public void ultrasonicSensorStartRanging(UltrasonicSensorControl sensor) {
    msg.ultrasonicSensorStartRanging(getDeviceId(sensor));
  }

  @Override
  // > ultrasonicSensorStopRanging/deviceId
  public void ultrasonicSensorStopRanging(UltrasonicSensorControl sensor) {
    msg.ultrasonicSensorStopRanging(getDeviceId(sensor));
  }

  public void uploadSketch(String arduinoPath) throws IOException {
    uploadSketch(arduinoPath, serial.getLastPortName());
  }

  public void uploadSketch(String arudinoPath, String comPort) throws IOException {
    uploadSketch(arudinoPath, comPort, getBoard());
  }

  static public String getBoardType(int boardId) {
    String boardName;
    switch (boardId) {
      case BOARD_TYPE_ID_MEGA:
        boardName = BOARD_TYPE_MEGA;
        break;
      case BOARD_TYPE_ID_UNO:
        boardName = BOARD_TYPE_UNO;
        break;
      case BOARD_TYPE_ID_ADK_MEGA:
        boardName = BOARD_TYPE_MEGA_ADK;
        break;
      case BOARD_TYPE_ID_NANO:
        boardName = BOARD_TYPE_NANO;
        break;
      case BOARD_TYPE_ID_PRO_MINI:
        boardName = BOARD_TYPE_PRO_MINI;
        break;
      default:
        // boardName = "unknown";
        boardName = BOARD_TYPE_UNO;
        break;
    }
    return boardName;
  }

  static public int getBoardTypeId(String boardName) {
    Integer boardId = null;
    switch (boardName) {
      case BOARD_TYPE_MEGA:
        boardId = BOARD_TYPE_ID_MEGA;
        break;
      case BOARD_TYPE_UNO:
        boardId = BOARD_TYPE_ID_UNO;
        break;
      case BOARD_TYPE_MEGA_ADK:
        boardId = BOARD_TYPE_ID_ADK_MEGA;
        break;
      case BOARD_TYPE_NANO:
        boardId = BOARD_TYPE_ID_NANO;
        break;
      case BOARD_TYPE_PRO_MINI:
        boardId = BOARD_TYPE_ID_PRO_MINI;
        break;
      default:
        // boardName = "unknown";
        boardId = BOARD_TYPE_ID_UNO;
        break;
    }
    return boardId;
  }

  public void uploadSketch(String arduinoIdePath, String port, String type) throws IOException {
    log.info("uploadSketch ({}, {}, {})", arduinoIdePath, port, type);

    if (!setArduinoPath(arduinoIdePath)) {
      return;
    }

    // hail mary - if we have no idea
    // guess uno
    if (type == null || type.equals("")) {
      type = BOARD_TYPE_UNO;
    }

    log.info("arduino IDE Path={}", arduinoIdePath);
    log.info("Port={}", port);
    log.info("type={}", type);
    /*
     * not needed if (arduinoIdePath != null &&
     * !arduinoIdePath.equals(ArduinoUtils.arduinoPath)) { this.arduinoPath =
     * arduinoIdePath; ArduinoUtils.arduinoPath = arduinoIdePath; save(); }
     */

    uploadSketchResult = String.format("Uploaded %s ", new Date());

    boolean connectedState = isConnected();
    try {

      if (connectedState) {
        log.info("disconnecting...");
        disconnect();
      }
      ArduinoUtils.uploadSketch(port, type.toLowerCase());

    } catch (Exception e) {
      log.info("ArduinoUtils threw trying to upload", e);
    }

    if (connectedState) {
      log.info("reconnecting...");
      serial.connect();
    }

    // perhaps you can reduce the inter-process information
    // to succeed | fail .. perhaps you can't
    // I would prefer transparency - send all output to the ui
    uploadSketchResult += ArduinoUtils.getOutput();

    log.info(uploadSketchResult);
    broadcastState();
  }

  /**
   * this is what Arduino firmware 'should' have done - a simplified
   * write(address, value) which follows the convention of 'all' device
   * operations at the lowest level
   * http://codewiki.wikidot.com/c:system-calls:write PinArrayControl method
   */
  @Override
  public void write(int address, int value) {
    info("write (%d,%d) to %s", address, value, serial.getName());
    PinDefinition pinDef = getPin(address);
    pinMode(address, "OUTPUT");
    if (pinDef.isPwm() && value > 1) { // CHEESEY HACK !!
      analogWrite(address, value);
    } else {
      digitalWrite(address, value);
    }
    // cache value
    pinDef.setValue(value);
  }

  public Map<String, DeviceMapping> getDeviceList() {
    return deviceList;
  }

  public void noAck() {
    log.error("no Ack we are resetting the serial port !");
    /*
     * String portName = getPortName(); disconnect(); sleep(1000);
     * connect(portName);
     */
  }

  public void publishMrlCommBegin(Integer version) {
    log.info("publishMrlCommBegin ({}) - going to sync", version);
    sync();
    if (mrlCommBegin > 0) {
      error("arduino %s has reset - does it have a separate power supply?", getName());
    }
    ++mrlCommBegin;
  }

  /**
   * DO NOT FORGET INSTALL AND VMARGS !!!
   * 
   * -Djava.library.path=libraries/native -Djna.library.path=libraries/native
   * -Dfile.encoding=UTF-8
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {
      
      // Platform.setVirtual(true);
      
      Runtime.main(new String[] { "--interactive", "--id", "id"});

      
      LoggingFactory.init(Level.INFO);
      // Platform.setVirtual(true);
      
      /*
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.setPort(8887);
      webgui.startService();
      */
      
      
      // Runtime.start("gui", "SwingGui");
      Serial.listPorts();

      Arduino hub = (Arduino) Runtime.start("hub", "Arduino");
      
      hub.connect("/dev/ttyACM0");

      // hub.enableAck(false);
      
      ServoControl sc = (ServoControl) Runtime.start("s1", "Servo");
      sc.setPin(3);
      hub.attach(sc);
      sc = (ServoControl) Runtime.start("s2", "Servo");
      sc.setPin(9);
      hub.attach(sc);
      

      // hub.enableAck(true);
      /*
       * sc = (ServoControl) Runtime.start("s3", "Servo"); sc.setPin(12);
       * hub.attach(sc);
       */

      log.info("here");
      // hub.connect("COM6"); // uno

      

      // hub.startTcpServer();

      boolean isDone = true;

      if (isDone) {
        return;
      }

      VirtualArduino vmega = null;

      vmega = (VirtualArduino) Runtime.start("vmega", "VirtualArduino");
      vmega.connect("COM7");
      Serial sd = (Serial) vmega.getSerial();
      sd.startTcpServer();

      // Runtime.start("webgui", "WebGui");

      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");

      if (mega.isVirtual()) {
        vmega = mega.getVirtual();
        vmega.setBoardMega();
      }

      // mega.getBoardTypes();
      // mega.setBoardMega();
      // mega.setBoardUno();
      mega.connect("COM7");

      /*
       * Arduino uno = (Arduino) Runtime.start("uno", "Arduino");
       * uno.connect("COM6");
       */

      // log.info("port names {}", mega.getPortNames());

      Servo servo = (Servo) Runtime.start("servo", "Servo");
      // servo.load();
      log.info("rest is {}", servo.getRest());
      servo.save();
      // servo.setPin(8);
      servo.attach(mega, 13);

      servo.moveTo(90.0);

      /*
       * servo.moveTo(3); sleep(300); servo.moveTo(130); sleep(300);
       * servo.moveTo(90); sleep(300);
       * 
       * 
       * // minmax checking
       * 
       * servo.invoke("moveTo", 120);
       */

      /*
       * mega.attach(servo);
       * 
       * servo.moveTo(3);
       * 
       * servo.moveTo(30);
       * 
       * mega.enablePin("A4");
       * 
       * // arduino.setBoardMega();
       * 
       * Adafruit16CServoDriver adafruit = (Adafruit16CServoDriver)
       * Runtime.start("adafruit", "Adafruit16CServoDriver");
       * adafruit.attach(mega); mega.attach(adafruit);
       */

      // servo.attach(arduino, 8, 90);

      // Runtime.start("webgui", "WebGui");
      // Service.sleep(3000);

      // remote.startListening();

      
      // Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  /**
   * stops the servo sweeping or moving with speed control
   */
  @Override
  public void onServoStop(ServoControl servo) {
    msg.servoStop(getDeviceId(servo));
  }
  
  

}