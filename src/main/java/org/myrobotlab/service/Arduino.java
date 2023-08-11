package org.myrobotlab.service;

import static org.myrobotlab.arduino.Msg.MAX_MSG_SIZE;
import static org.myrobotlab.arduino.Msg.MRLCOMM_VERSION;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.arduino.DeviceSummary;
import org.myrobotlab.arduino.Msg;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.i2c.I2CBus;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.abstracts.AbstractMicrocontroller;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.data.DeviceMapping;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.MrlCommPublisher;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoEvent;
import org.myrobotlab.service.interfaces.ServoStatusPublisher;
import org.myrobotlab.service.interfaces.UltrasonicSensorControl;
import org.myrobotlab.service.interfaces.UltrasonicSensorController;
import org.slf4j.Logger;

public class Arduino extends AbstractMicrocontroller<ArduinoConfig> implements I2CBusController, I2CController, SerialDataListener, ServoController, MotorController, NeoPixelController,
    UltrasonicSensorController, PortConnector, RecordControl, PortListener, PortPublisher, EncoderController, PinArrayPublisher, MrlCommPublisher, ServoStatusPublisher {

  transient public final static Logger log = LoggerFactory.getLogger(Arduino.class);

  public static class I2CDeviceMap {
    public String busAddress;
    public transient I2CControl control;
    public String deviceAddress;
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
  public static final int PULLUP = 0x2;

  private static final long serialVersionUID = 1L;

  String aref;

  /**
   * board info "from" MrlComm - which can be different from what the user say's
   * it is - if there is a difference the "user" should be notified - but not
   * forced to use the mrlBoardInfo.
   */
  volatile BoardInfo boardInfo = null;

  volatile BoardInfo lastBoardInfo = null;

  boolean boardInfoEnabled = true;

  private long boardInfoRequestTs;

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

  transient Mapper motorPowerMapper = new MapperLinear(-1.0, 1.0, -255.0, 255.0);

  // make final - if not "connected" log error but don't allow Arduino NPEs
  public final transient Msg msg = new Msg(this, null);

  Integer nextDeviceId = 0;

  /**
   * Serial service - the Arduino's serial connection FIXME - remove this - its
   * not pub/sub !
   */
  transient Serial serial;

  /**
   * virtual arduino for testing purposes
   */
  transient private VirtualArduino virtual;

  int mrlCommBegin = 0;

  private volatile boolean syncInProgress = false;

  /**
   * the port the user attempted to connect to
   */
  String port;

  public Arduino(String n, String id) {
    super(n, id);

    // board is set
    // now we can create a pin list
    getPinList();

    // get list of board types
    getBoardTypes();

    // add self as an attached device
    // to handle pin events
    attachDevice(this, (Object[]) null);
  }

  // > analogWrite/address/value
  public void analogWrite(int address, int value) {
    log.info("analogWrite({},{})", address, value);
    msg.analogWrite(address, value);
    PinDefinition pinDef = addressIndex.get(address);
    pinDef.setValue(value);
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
   * 
   * FIXME - each one of these typed functions could simply provide the name of the
   * interface that desires to attach.  Then routing would be done easily by
   * invoke("attach" + InterfaceName, name)
   * 
   * If further refactored, the interface might be able to provide the implementation of
   * setting up pub/sub/listeners
   * 
   */
  @Override
  public void attach(String name) throws Exception {
    ServiceInterface service = Runtime.getService(name);
    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      attachServoControl((ServoControl) service);
      return;
    } else if (MotorControl.class.isAssignableFrom(service.getClass())) {
      attachMotorControl((MotorControl) service);
      return;
    } else if (service instanceof UltrasonicSensorControl) {
      attach(service);
      return;
    } else if (service instanceof EncoderControl) {
      attachEncoderControl((EncoderControl) service);
      return;
    } else if (service instanceof PinArrayListener) {
      attachPinArrayListener((PinArrayListener) service);
      return;
    } else if (service instanceof PinListener) {
      attachPinListener((PinListener) service);
      return;
    }
    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    servo.setPin(pin);
    attachServoControl(servo);
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

    if (deviceList.containsKey(device.getName())) {
      log.warn("device {} already attached to {}", device.getName(), getName());
      return deviceList.get(device.getName());
    }

    DeviceMapping map = new DeviceMapping(nextDeviceId, device);
    log.info("DEVICE LIST PUT ------ Name: {} Class: {} Map: {}", device.getName(), device.getClass().getSimpleName(), map);
    deviceList.put(device.getName(), map);
    deviceIndex.put(nextDeviceId, map);
    ++nextDeviceId;
    return map;
  }

  /**
   * Attach an encoder to the arduino
   * 
   * @param encoder
   *          - the encoder control to attach
   */
  @Override
  public void attachEncoderControl(EncoderControl encoder) {

    if (encoder == null) {
      error("%s.attachEncoderControl(null)", getName());
      return;
    }

    if (deviceList.containsKey(encoder.getName())) {
      log.info("already attached");
      return;
    }

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
    // attach the virtual representation of the device and get an id for it.
    DeviceMapping m = attachDevice(encoder, new Object[] { address });
    // send the attach method with our device id.
    msg.encoderAttach(m.getId(), type, address);

    encoder.attachEncoderController(this);

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
      i2cBus = new I2CBus(String.format("I2CBus%s", control.getBus()));
      i2cBusAttach(i2cBus, Integer.parseInt(control.getBus()));
    }

    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    String key = String.format("%s.%s", control.getBus(), control.getAddress());
    I2CDeviceMap devicedata = new I2CDeviceMap();
    if (i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getBus(), control.getAddress(), control.getName());
    } else {
      devicedata.busAddress = control.getBus();
      devicedata.deviceAddress = control.getAddress();
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

    // int pin = (servo.getPin() == null)?-1:getAddress(servo.getPin());
    int pin = getAddress(servo.getPin());
    // targetOutput is never null and is the input requested angle in degrees
    // for the servo.
    // defaulting to the rest angle.
    double targetOutput = servo.getTargetOutput();
    double speed = (servo.getSpeed() == null) ? -1 : servo.getSpeed();

    // add a device to our deviceList
    DeviceMapping dm = attachDevice(servo, new Object[] { pin, targetOutput, speed });

    if (isConnected()) {
      int uS = degreeToMicroseconds(servo.getTargetOutput());
      msg.servoAttach(dm.getId(), pin, uS, (int) speed, servo.getName());
      msg.servoAttachPin(dm.getId(), pin);
    }
    if (!servo.isAttached(getName())) {
      send(servo.getName(), "attach", getName());
    }
  }

  /**
   * attach a pin listener who listens to a specific pin
   */
  public void attachPinListener(PinListener listener) {
    if (listener == null) {
      error("attachPinListener(null)");
      return;
    }
    super.attachPinListener(listener);
    // add a device to our deviceList
    // DeviceMapping dm =
    attachDevice((Attachable) listener, new Object[] { listener.getPin() });

  }

  /**
   * reattach - if the serial connection breaks or gets disconnected and we have
   * a device list - when the serial connection is established again we go
   * through all the devices and make sure any initialization communication to
   * mrlcomm is done to sync the states
   * 
   * @param dm
   */
  public void reattach(DeviceMapping dm) {
    log.info("reattaching {}", dm);
    Attachable attachable = dm.getDevice();
    if (attachable.getName().equals(getName())) {
      // re-attaching ourselves only requires that a record is in
      // the device list ... so we don't need to do anything
      return;
    } else if (attachable instanceof ServoControl) {
      ServoControl servo = (ServoControl) attachable;
      int uS = degreeToMicroseconds(servo.getTargetOutput());
      double speed = (servo.getSpeed() == null) ? -1 : servo.getSpeed();
      int pin = getAddress(servo.getPin());
      log.info("================ re-attaching {} {} {} ================", servo.getName(), dm.getId(), pin);
      msg.servoAttach(dm.getId(), pin, uS, (int) speed, servo.getName()); //
      if (servo.isEnabled()) {
        msg.servoAttachPin(dm.getId(), pin);
      }
    } else if (attachable instanceof UltrasonicSensorControl) {
      log.warn("UltrasonicSensorControl not implemented");
      // reattach logic
      // } else if (attachable instanceof Pir) { Pir is a PinListener
      // reattach logic - FIXME Pir has no Control interface :(
    } else if (attachable instanceof PinListener) {
      PinListener pl = (PinListener) attachable;
      attachPinListener(pl);

      // on reattach get back to its previous state enabled/disabled
      if (attachable instanceof Pir) {
        Pir pir = (Pir) attachable;
        if (pir.isEnabled()) {
          pir.enable();
        }
      }

    } else if (attachable instanceof I2CControl) {
      error("I2CControl sync not implemented");
    } else {
      error("cannot reattach device of type %s do not know how", dm.getDevice().getClass().getSimpleName());
    }
  }

  @Override
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

    if (port == null) {
      warn("%s attempted to connect with a null port", getName());
      return;
    }

    serial = (Serial) startPeer("serial");
    msg.setSerial(serial);
    serial.addByteListener(this);

    // test to see if we've been started. the serial might be null
    this.port = port;

    try {

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
    if (syncInProgress) {
      log.warn("Alreadying calling sync!  Skipping this request");
      return;
    }
    syncInProgress = true;
    log.warn("================================ sync !!! ==============================");
    try {
      for (DeviceMapping device : deviceList.values()) {
        reattach(device);
      }

      List<PinDefinition> list = getPinList();
      for (PinDefinition pindef : list) {
        if (pindef.isEnabled()) {
          enablePin(pindef.getPinName(), pindef.getPollRate());
        }
      }

    } catch (Exception e) {
      log.error("sync threw", e);
    }
    syncInProgress = false;
    log.info("Sync completed");

  }

  // > customMsg/[] msg
  public void customMsg(int... params) {
    msg.customMsg(params);
  }

  @Override
  public void detach() {
    // make list copy - to iterate without fear of thread or modify issues
    ArrayList<DeviceMapping> newList = new ArrayList<>(deviceIndex.values());
    log.info("detaching all devices");
    /*
     * DOESN'T MATTER IF CONNECTED - IF RECONNECT ARDUINO DEMANDS ITS CURRENT
     * STATE ONTO MrlComm if (isConnected()) { for (DeviceMapping dm: newList) {
     * if (dm.getDevice().getName().equals(getName())) { continue; }
     * detach(dm.getDevice()); sleep(50); } }
     */
    deviceIndex.clear();
    deviceList.clear();
  }

  // @Override
  // > deviceDetach/deviceId
  @Override
  public void detach(Attachable device) {
    super.detach(device);
    if (device == null) {
      return;
    }

    log.info("{} detaching {}", getName(), device.getName());
    // if this service doesn't think its attached, we are done
    if (!isAttached(device)) {
      log.info("device {} not attached", device.getName());
      return;
    }

    log.info("detaching device {}", device.getName());
    Integer id = getDeviceId(device);
    if (id != null && msg != null) {
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
   * 
   * @param address
   *          the address
   * @param value
   *          the value to write
   */
  public void digitalWrite(int address, int value) {
    log.info("digitalWrite {} {}", address, value);
    msg.digitalWrite(address, value);
    PinDefinition pinDef = addressIndex.get(address);
    pinDef.setValue(value);
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
    if (pinDef == null) {
      warn("pin definition %s does not exist", pinName);
      return;
    }

    pinDef.setEnabled(false);
    msg.disablePin(pinDef.getAddress());
  }

  /**
   * disable all pins
   */
  @Override
  public void disablePins() {
    msg.disablePins();
  }

  @Override
  public void disconnect() {
    // FIXED - all don in 'onDisconnect()'
    // enableBoardInfo(false);
    // boardInfo is not valid after disconnect
    // because we might be connecting to a different Arduino
    // boardInfo.reset();
    if (serial != null) {
      serial.disconnect();
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

    @Override
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

  public void enablePin(String pin) {
    PinDefinition pinDef = getPin(pin);
    enablePin(pinDef.getPin(), 10);
  }

  @Override
  @Deprecated /* use enablePin(String) */
  public void enablePin(int address) {
    PinDefinition pinDef = getPin(address);
    enablePin(pinDef.getPin(), 1);
  }

  // > enablePin/address/type/b16 rate
  @Override
  @Deprecated /* use enablePin(String, int) */
  public void enablePin(int address, int rate) {
    PinDefinition pinDef = getPin(address);
    enablePin(pinDef.getPin(), rate);
  }

  /**
   * start polling reads of selected pin enablePin/address/type/b16 rate
   */
  @Override
  public void enablePin(String pin, int rate) {
    if (isConnected()) {
      PinDefinition pinDef = getPin(pin);
      msg.enablePin(pinDef.getAddress(), getMrlPinType(pinDef), rate);
      pinDef.setEnabled(true);
      pinDef.setPollRate(rate);
      invoke("publishPinDefinition", pinDef); // broadcast pin change
    }
  }

  public String getAref() {
    return aref;
  }

  @Override
  public Set<String> getAttached() {
    Set<String> ret = new HashSet<>();
    // all services which use subscriptions
    ret.addAll(super.getAttached());
    // services which some do direct calls
    ret.addAll(deviceList.keySet());
    return ret;
  }

  @Deprecated /* what's the point? - get the list from getAttached() */
  public int getAttachedCount() {
    return getAttached().size();
  }

  /**
   * Heart-beat method on time, driven by the Arduino service to get information
   * from the board its currently connected. This is the "last" boardInfo
   * returned from the task inserted with addTask("getBoardInfo", 1000, 0,
   * "sendBoardInfoRequest");
   * 
   * getBoardInfo
   */
  @Override
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
   * @param pin
   *          the pin definition
   * @return the type of pin
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
    if (board == null) {
      return new ArrayList<PinDefinition>();
    }

    // if no change - just return the values
    if ((addressIndex != null && board.contains("mega") && addressIndex.size() == 70) || (addressIndex != null && !board.contains("mega") && addressIndex.size() == 20)) {
      return new ArrayList<PinDefinition>(addressIndex.values());
    }

    // create 2 indexes for fast retrieval
    // based on "name" or "address"
    pinIndex.clear();
    addressIndex.clear();

    List<PinDefinition> pinList = new ArrayList<PinDefinition>();

    if (board.contains("mega")) {
      for (int i = 0; i < 70; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);
        // begin wacky pin def logic
        String pinName = null;
        if (i == 0 || i == 15 || i == 17 || i == 19) {
          pindef.setRx(true);
        }
        if (i == 1 || i == 14 || i == 16 || i == 18) {
          pindef.setTx(true);
        }
        if (i == 20) {
          pindef.setSda(true);
        }
        if (i == 21) {
          pindef.setScl(true);
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
        pindef.setMode("INPUT");
        pindef.setValue(0);
        pindef.setPinName(pinName);
        pindef.setAddress(i);
        pinIndex.put(pinName, pindef);
        addressIndex.put(pindef.getAddress(), pindef);
        pinList.add(pindef);
      }
    } else {
      int pinCount = 20;
      if (board.contains("nano")) {
        pinCount = 22;
      }
      for (int i = 0; i < pinCount; ++i) {
        PinDefinition pindef = new PinDefinition(getName(), i);
        String pinName = null;
        if (i == 0) {
          pindef.setRx(true);
        }
        if (i == 1) {
          pindef.setTx(true);
        }
        if (i == 18) {
          pindef.setSda(true);
        }
        if (i == 19) {
          pindef.setScl(true);
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
        pinIndex.put(pinName, pindef);
        addressIndex.put(pindef.getAddress(), pindef);
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

  @Override
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

  /**
   * Get the serial service for this device
   * 
   * @return - serial service
   */
  public Serial getSerial() {
    return serial;
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

  @Override
  public boolean isAttached(Attachable service) {
    return getAttached().contains(service.getName());
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

  @Override
  public synchronized void onBytes(byte[] bytes) {
    // log.info("On Bytes called in Arduino. {}", bytes);
    // These bytes arrived from the serial port data, push them down into the
    // msg parser.
    // if a full message is detected, the publish(Function) method will be
    // directly called on
    // this arduino instance.
    msg.onBytes(bytes);
  }

  @Override
  public synchronized void onConnect(String portName) {
    // Pass this serial port notification down to the msg parser
    msg.onConnect(portName);
    log.info("{} onConnect for port {}", getName(), portName);
    info("%s connected to %s", getName(), portName);
    // chained...
    invoke("publishConnect", portName);
    
    broadcastState();
  }

  public void onCustomMsg(Integer ax, Integer ay, Integer az) {
    log.info("onCustomMsg");
  }

  @Override
  public void onDisconnect(String portName) {
    msg.onDisconnect(portName);
    info("%s disconnected from %s", getName(), portName);
    enableBoardInfo(false);
    // chained...
    invoke("publishDisconnect", portName);
  }

  public String getBase64ZippedMrlComm() {
    return CodecUtils.toBase64(getZippedMrlComm());
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

  /**
   * // > pinMode/pin/mode
   */
  @Deprecated /* use pinMode(String, String */
  public void pinMode(int address, String modeStr) {
    if (modeStr.equalsIgnoreCase("OUTPUT")) {
      pinMode(address, Arduino.OUTPUT);
    } else if (modeStr.equalsIgnoreCase("PULLUP")) {
      pinMode(address, Arduino.PULLUP);
    } else {
      // default arduino pin mode
      pinMode(address, Arduino.INPUT);
    }
  }

  // the "important pinMode" - with types Arduino supports
  public void pinMode(int address, int mode) {
    log.info("pinMode {} {}", address, mode);
    msg.pinMode(address, mode);
    PinDefinition pinDef = addressIndex.get(address);
    pinDef.setMode(mode == Arduino.OUTPUT ? "OUTPUT" : "INPUT");
  }

  public void pinMode(String pin, int mode) {
    PinDefinition pinDef = getPin(pin);
    pinMode(pinDef.getAddress(), mode);
  }

  /**
   * With Arduino we want to be able to do pinMode("D7", "INPUT"), but it should
   * not be part of the PinArrayControl interface - because when it comes down
   * to it .. a pin MUST ALWAYS have an address regardless what you label or
   * name it...
   * 
   */
  @Override
  public void pinMode(String pin, String mode) {
    PinDefinition pinDef = getPin(pin);
    pinMode(pinDef.getAddress(), mode);
  }

  // < publishAck/function
  @Override
  public void publishAck(Integer function/* byte */) {
    if (msg.debug) {
      log.info("{} Message Ack received: =={}==", getName(), Msg.methodToString(function));
    }
  }

  // < publishBoardInfo/version/boardType/b16 microsPerLoop/b16 sram/[]
  // deviceSummary
  @Override
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

    // TODO: consider, can we really just re-sync when we see begin only.. ?
    // feels better/safer.
    // if (boardInfo != null) {
    // DeviceSummary[] ds = boardInfo.getDeviceSummary();
    // if (deviceList.size() - 1 > ds.length) { /* -1 for self */
    // log.info("Invoking Sync DeviceList: {} and DeviceSummary: {}",
    // deviceList, ds);
    // invoke("sync");
    // }
    // }

    // we send here - because this is a "command" message, and we don't want the
    // possibility of
    // block this "status" msgs
    lastBoardInfo = boardInfo;
    return boardInfo;
  }

  @Override
  public String publishConnect(String portName) {
    return portName;
  }

  // < publishCustomMsg/[] msg
  @Override
  public int[] publishCustomMsg(int[] msg/* [] */) {
    return msg;
  }

  // < publishDebug/str debugMsg
  @Override
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
   */
  @Override
  public void publishEcho(float myFloat, int myByte, float secondFloat) {
    log.info("myFloat {} {} {} ", myFloat, myByte, secondFloat);
  }

  @Override
  public EncoderData publishEncoderData(EncoderData data) {
    log.info("Publish Encoder Data {}", data);
    return data;
  }

  // callback for generated method from arduinoMsg.schema
  @Override
  public EncoderData publishEncoderData(Integer deviceId, Integer position) {
    // Also need to log this

    EncoderControl ec = (EncoderControl) getDevice(deviceId);
    String pin = null;
    Double angle = null;
    if (ec instanceof Amt203Encoder) {
      // type = 0;
      pin = ((Amt203Encoder) ec).getPin();
    } else if (ec instanceof As5048AEncoder) {
      // type = 1;
      pin = ((As5048AEncoder) ec).getPin();
      angle = 360.0 * position / ((As5048AEncoder) ec).resolution;
      log.info("Angle : {}", angle);
    } else {
      error("unknown encoder type {}", ec.getClass().getName());
    }

    EncoderData data = new EncoderData(ec.getName(), pin, position, angle);
    // log.info("Publish Encoder Data Raw {}", data);

    // TODO: all this code needs to move out of here!
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
  @Override
  public void publishI2cData(Integer deviceId, int[] data) {
    log.info("publishI2cData");
    i2cReturnData(data);
  }

  /**
   * error from mrlcom in string form
   * 
   * @param errorMsg
   *          a string representing the error message
   * @return the published error message
   */
  // < publishMRLCommError/str errorMsg
  @Override
  public String publishMRLCommError(String errorMsg/* str */) {
    warn("MrlCommError: " + errorMsg);
    log.error("MRLCommError: {}", errorMsg);
    return errorMsg;
  }

  // < publishPinArray/[] data
  @Override
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
      invoke("publishPin", pinData);
    }
    return pinArray;
  }

  public List<String> publishPortNames(List<String> portNames) {
    return portNames;
  }

  /**
   * FIXME - I bet this doesnt work - test it
   * 
   */
  @Override
  public SerialRelayData publishSerialData(Integer deviceId, int[] data) {
    SerialRelayData serialData = new SerialRelayData(deviceId, data);
    return serialData;
  }

  @Override
  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos) {
    if (getDevice(deviceId) != null) {
      Attachable attachable = getDevice(deviceId);
      if (eventType == 0) {
        // ((ServoStatusPublisher)
        // getDevice(deviceId)).publishServoStarted(getDevice(deviceId).getName());
        // FIXME - getCurrentOutputPos
        broadcast("publishServoStarted", getDevice(deviceId).getName(), ((ServoControl) attachable).getCurrentOutputPos());
      } else if (eventType == 1) {
        // ((ServoStatusPublisher)
        // getDevice(deviceId)).publishServoStopped(getDevice(deviceId).getName());

        // FIXME - getCurrentOutputPos
        broadcast("publishServoStopped", getDevice(deviceId).getName(), ((ServoControl) attachable).getCurrentOutputPos());
      } else {
        log.error("unknown servo event type {}", eventType);
      }
      log.debug("publishServoEvent deviceId {} event {} currentPos {}", deviceId, eventType, currentPos);
    } else {
      error("no servo found at device id %d", deviceId);
    }
    return currentPos;
  }

  // FIXME should be in Control interface - for callback
  // < publishUltrasonicSensorData/deviceId/b16 echoTime
  @Override
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

    poller.stop();

    // SHUTDOWN ACKING - use case - port no longer exists
    if (msg != null) {
      msg.enableAck(false);
    }

    if (virtual != null) {
      virtual.releaseService();
    }

    // remove all devices
    disconnect();
  }

  /**
   * resets both MrlComm-land &amp; Java-land
   */
  @Override
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
  @Override
  public void onServoDisable(String servoName) {
    if (!isConnected()) {
      warn("Arduino cannot set speed when not connected - connected %b", isConnected());
      return;
    }

    Integer id = getDeviceId(servoName);
    if (id != null) {
      msg.servoDetachPin(id);
    }
  }

  @Override
  public void onServoEnable(String servoName) {
    if (!isConnected()) {
      warn("Arduino cannot set speed when not connected - connected %b", isConnected());
      return;
    }

    Integer deviceId = getDeviceId(servoName);
    if (deviceId == null) {
      log.warn("servoEnable servo {} does not have a corresponding device currently - did you attach?", servoName);
      return;
    }
    if (isConnected()) {
      ServoControl sc = (ServoControl) Runtime.getService(servoName);
      msg.servoAttachPin(deviceId, getAddress(sc.getPin()));
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
  public void onServoMoveTo(ServoMove move) {
    if (!isConnected()) {
      warn("Arduino cannot set speed when not connected - connected %b", isConnected());
      return;
    }

    Integer deviceId = getDeviceId(move.name);
    if (deviceId == null) {
      log.warn("servoMoveTo servo {} does not have a corresponding device currently - did you attach?", move.name);
      return;
    }
    // getTargetOutput ALWAYS ALWAYS Degrees !
    // so we convert to microseconds
    int us = degreeToMicroseconds(move.outputPos);
    log.debug("servoMoveToMicroseconds servo {} id {} {}->{} us", move.name, deviceId, move.outputPos, us);
    msg.servoMoveToMicroseconds(deviceId, us);
  }

  @Override
  // > servoSetVelocity/deviceId/b16 velocity
  public void onServoSetSpeed(ServoSpeed servoSpeed) {

    if (servoSpeed == null) {
      log.warn("servo speed cannot be null");
      return;
    }
    
    if (!isConnected()) {
      log.info("Arduino cannot set speed of %s when not connected", servoSpeed.name);
      return;
    }

    int speed = -1;
    Servo servo = (Servo) Runtime.getService(servoSpeed.name);
    if (servoSpeed.speed != null) {
      speed = servoSpeed.speed.intValue();
    }
    log.debug("servoSetVelocity {} id {} velocity {}", servo.getName(), getDeviceId(servo), speed);
    Integer id = getDeviceId(servo);
    if (id == null) {
      log.error("{} has null deviceId", servo);
      return;
    }
    msg.servoSetVelocity(id, speed);
  }

  /**
   * On standard servos a parameter value of 1000 is fully counter-clockwise,
   * 2000 is fully clockwise, and 1500 is in the middle.
   */
  @Override
  // > servoWriteMicroseconds/deviceId/b16 ms
  public void onServoWriteMicroseconds(ServoControl servo, int uS) {
    if (!isConnected()) {
      warn("Arduino cannot set speed when not connected - connected %b msg %b", isConnected());
      return;
    }

    int deviceId = getDeviceId(servo);
    log.debug("writeMicroseconds {} {} id {}", servo.getName(), uS, deviceId);
    msg.servoMoveToMicroseconds(deviceId, uS);
  }

  public void setAref(String aref) {
    if (!isConnected()) {
      warn("Arduino cannot set speed when not connected - connected %b msg %b", isConnected());
      return;
    }
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
   * TODO - implement on MrlComm side ... or remove completely
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

  /**
   * this is what Arduino firmware 'should' have done - a simplified
   * write(address, value) which follows the convention of 'all' device
   * operations at the lowest level
   * http://codewiki.wikidot.com/c:system-calls:write PinArrayControl method
   */
  @Override
  public void write(int address, int value) {
    info("write (%d,%d)", address, value);
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

  @Override
  public void ackTimeout() {
    log.warn("{} Ack Timeout seen.  TODO: consider resetting the com port {}, reconnecting and re syncing all devices.", getName(), port);
  }

  @Override
  public void publishMrlCommBegin(Integer version) {
    // If we were already connected up and clear to send.. this is a problem..
    // it means the board was reset on it.
    if (mrlCommBegin > 0) {
      error("arduino %s has reset - does it have a separate power supply?", getName());
      // At this point we need to reset!
      mrlCommBegin = 0;
    }
    ++mrlCommBegin;
    // log.info("Skipping Sync! TODO: uncomment me.");
    // This needs to be non-blocking
    // If we have devices, we need to sync them.
    // The device list always has "Arduino" in it for some reason..
    if (deviceList.size() > 1) {
      log.info("Need to sync devices to mrlcomm. Num Devices: {} Devices: {}", deviceList.size(), deviceList);
      sendAsync(getName(), "sync");
    } else {
      log.info("no devices to sync, clear to resume.");
    }
  }

  /**
   * stops the servo sweeping or moving with speed control
   */
  @Override
  public void onServoStop(ServoControl servo) {
    log.debug("servo {}", servo.getName());
    Integer id = getDeviceId(servo);
    if (id != null && msg != null) {
      msg.servoStop(id);
    }
  }

  @Override
  public ServoEvent publishServoStarted(String name, Double position) {
    log.debug("CONTROLLER SERVO_STARTED {}", name);
    return new ServoEvent(name, position);
  }

  @Override
  public ServoEvent publishServoStopped(String name, Double position) {
    log.debug("CONTROLLER SERVO_STOPPED {} {}", name, position);
    return new ServoEvent(name, position);
  }

  @Override
  public void neoPixelAttach(String name, int pin, int numberOfPixels, int depth) {
    if (deviceList.containsKey(name)) {
      log.info("neopixel {} already attached", name);
      return;
    }
    ServiceInterface neopixel = Runtime.getService(name);
    DeviceMapping dm = attachDevice(neopixel, null);
    msg.neoPixelAttach(dm.getId(), pin, numberOfPixels, depth);
  }

  @Override
  public void neoPixelWriteMatrix(String neopixel, int[] buffer) {
    // 64 byte message size limit (including 3 byte header) !!! - so we bucket
    // them chunks
    int segments = (int) Math.ceil(buffer.length / 50.0);
    for (int i = 0; i < segments; ++i) {
      int begin = i * 50;
      int end = Math.min(begin + 49, buffer.length);
      msg.neoPixelWriteMatrix(getDeviceId(neopixel), Arrays.copyOfRange(buffer, begin, end));
    }
  }

  @Override
  public void neoPixelSetAnimation(String neopixel, int animation, int red, int green, int blue, int white, int speed) {
    msg.neoPixelSetAnimation(getDeviceId(neopixel), animation, red, green, blue, white, speed);
  }

  @Override
  public void neoPixelFill(String neopixel, int beginAddress, int count, int red, int green, int blue, int white) {
    msg.neoPixelFill(getDeviceId(neopixel), beginAddress, count, red, green, blue, white);
  }

  @Override
  public void neoPixelSetBrightness(String neopixel, int brightness) {
    msg.neoPixelSetBrightness(getDeviceId(neopixel), brightness);
  }

  @Override
  public void neoPixelClear(String neopixel) {
    msg.neoPixelClear(getDeviceId(neopixel));
  }

  @Override
  public ArduinoConfig getConfig() {
    ArduinoConfig c = super.getConfig();

    // FIXME "port" shouldn't exist only config.port !
    c.port = port;
    c.connect = isConnected();

    return c;
  }

  @Override
  public ArduinoConfig apply(ArduinoConfig c) {
    ArduinoConfig config = (ArduinoConfig) super.apply(c);

    if (msg == null) {
      serial = (Serial) startPeer("serial");
      if (serial == null) {
        log.error("serial is null");
      }
      msg.setSerial(serial);
      serial.addByteListener(this);
    } else {
      // TODO: figure out why this gets called so often.
      log.info("Init serial we already have a msg class.");
    }

    if (config.connect && config.port != null) {
      connect(config.port);
    }

    return config;
  }

  /**
   * DO NOT FORGET INSTALL AND VMARGS !!!
   * 
   * -Djava.library.path=libraries/native -Djna.library.path=libraries/native
   * -Dfile.encoding=UTF-8
   * 
   * @param args
   *          command line args
   * 
   */
  public static void main(String[] args) {
    try {

      // Platform.setVirtual(true);

      LoggingFactory.init(Level.INFO);

      Runtime.start("arduino", "Arduino");
      Runtime.start("python", "Python");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      boolean isDone = true;

      if (isDone) {
        return;
      }
      // Platform.setVirtual(true);

      /*
       * WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
       * webgui.autoStartBrowser(false); webgui.setPort(8887);
       * webgui.startService();
       */

      // Runtime.start("gui", "SwingGui");
      Serial.listPorts();

      Arduino hub = (Arduino) Runtime.start("controller", "Arduino");
      Runtime.start("pir", "Pir");

      hub.connect("/dev/ttyACM0");

      // hub.enableAck(false);

      ServoControl sc = (ServoControl) Runtime.start("s1", "Servo");
      sc.setPin(3);
      hub.attach(sc);
      sc = (ServoControl) Runtime.start("s2", "Servo");
      sc.setPin(9);
      hub.attach(sc);

      hub.detach();

      // hub.enableAck(true);
      /*
       * sc = (ServoControl) Runtime.start("s3", "Servo"); sc.setPin(12);
       * hub.attach(sc);
       */

      log.info("here");
      // hub.connect("COM6"); // uno

      // hub.startTcpServer();

      VirtualArduino vmega = null;

      vmega = (VirtualArduino) Runtime.start("vmega", "VirtualArduino");
      vmega.connect("COM7");
      Serial sd = vmega.getSerial();
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
      servo.attach(mega);

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

}
