package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

/**
 * MPR121 Proximity Capacitive Touch Sensor Controller
 * 
 * @author Mats
 * 
 *         https://www.sparkfun.com/datasheets/Components/MPR121.pdf
 * 
 */
public class Mpr121 extends Service implements I2CControl, PinArrayControl {
  /**
   * Publisher - Publishes pin data at a regular interval
   */
  public class Publisher extends Thread {

    public Publisher(String name) {
      super(String.format("%s.publisher", name));
    }

    @Override
    public void run() {

      log.info(String.format("New publisher instance started at a sample frequency of %s Hz", sampleFreq));
      long sleepTime = 1000 / (long) sampleFreq;
      isPublishing = true;
      try {
        while (isPublishing) {
          Thread.sleep(sleepTime);
          publishPinData();
        }

      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          log.info("Shutting down Publisher");
        } else {
          isPublishing = false;
          log.error(String.format("publisher threw %s",e));
        }
      }
    }

    void publishPinData() {

      PinData[] pinArray = new PinData[pinDataCnt];
      getTouched(); // Refresh the pinData

      for (int i = 0; i < pinArray.length; ++i) {
        PinData pinData = new PinData(i, read(i));
        pinArray[i] = pinData;
        int address = pinData.address;

        // handle individual pins
        if (pinListeners.containsKey(address)) {
          List<PinListener> list = pinListeners.get(address);
          for (int j = 0; j < list.size(); ++j) {
            PinListener pinListner = list.get(j);
            if (pinListner.isLocal()) {
              pinListner.onPin(pinData);
            } else {
              invoke("publishPin", pinData);
            }
          }
        }
      }

      // publish array
      invoke("publishPinArray", new Object[] { pinArray });
    }
  }

  // Publisher
  boolean isPublishing = false;
  transient Publisher publisher = null;
  int pinDataCnt = 12;
  //

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Mpr121.class);
  public transient I2CController controller;

  public List<String> deviceAddressList = Arrays.asList("0x5A", "0x5B", "0x5C", "0x5D");

  public String deviceAddress = "0x5A";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public List<String> controllers;
  public String controllerName;
  public boolean isAttached = false;

  /*
   * =========================================================================
   * MPR121 Registers
   * -----------------------------------------------------------------------
   */
  static final int TOUCH_STATUS0 = 0x00;
  static final int TOUCH_STATUS1 = 0x01;
  static final int OOR_STATUS0 = 0x02;
  static final int OOR_STATUS1 = 0x03;

  static final int ELE0_FILTERED_DATA_LSB = 0x04;
  static final int ELE0_FILTERED_DATA_MSB = 0x05;
  static final int ELE1_FILTERED_DATA_LSB = 0x06;
  static final int ELE1_FILTERED_DATA_MSB = 0x07;
  static final int ELE2_FILTERED_DATA_LSB = 0x08;
  static final int ELE2_FILTERED_DATA_MSB = 0x09;
  static final int ELE3_FILTERED_DATA_LSB = 0x0A;
  static final int ELE3_FILTERED_DATA_MSB = 0x0B;
  static final int ELE4_FILTERED_DATA_LSB = 0x0C;
  static final int ELE4_FILTERED_DATA_MSB = 0x0D;
  static final int ELE5_FILTERED_DATA_LSB = 0x0E;
  static final int ELE5_FILTERED_DATA_MSB = 0x0F;
  static final int ELE6_FILTERED_DATA_LSB = 0x10;
  static final int ELE6_FILTERED_DATA_MSB = 0x11;
  static final int ELE7_FILTERED_DATA_LSB = 0x12;
  static final int ELE7_FILTERED_DATA_MSB = 0x13;
  static final int ELE8_FILTERED_DATA_LSB = 0x14;
  static final int ELE8_FILTERED_DATA_MSB = 0x15;
  static final int ELE9_FILTERED_DATA_LSB = 0x16;
  static final int ELE9_FILTERED_DATA_MSB = 0x17;
  static final int ELE10_FILTERED_DATA_LSB = 0x18;
  static final int ELE10_FILTERED_DATA_MSB = 0x19;
  static final int ELE11_FILTERED_DATA_LSB = 0x1A;
  static final int ELE11_FILTERED_DATA_MSB = 0x1B;
  static final int ELEPROX_FILTERED_DATA_LSB = 0x1C;
  static final int ELEPROX_FILTERED_DATA_MSB = 0x1D;

  static final int ELE0_BASELINE_VALUE = 0x1E;
  static final int ELE1_BASELINE_VALUE = 0x1F;
  static final int ELE2_BASELINE_VALUE = 0x20;
  static final int ELE3_BASELINE_VALUE = 0x21;
  static final int ELE4_BASELINE_VALUE = 0x22;
  static final int ELE5_BASELINE_VALUE = 0x23;
  static final int ELE6_BASELINE_VALUE = 0x24;
  static final int ELE7_BASELINE_VALUE = 0x25;
  static final int ELE8_BASELINE_VALUE = 0x26;
  static final int ELE9_BASELINE_VALUE = 0x27;
  static final int ELE10_BASELINE_VALUE = 0x28;
  static final int ELE11_BASELINE_VALUE = 0x29;
  static final int ELEPROX_BASELINE_VALUE = 0x2A;

  static final int MHD_RISING = 0x2B;
  static final int NHD_RISING = 0x2C;
  static final int NCL_RISING = 0x2D;
  static final int FDL_RISING = 0x2E;

  static final int MHD_FALLING = 0x2F;
  static final int NHD_FALLING = 0x30;
  static final int NCL_FALLING = 0x31;
  static final int FDL_FALLING = 0x32;

  static final int NHD_TOUCHED = 0x33;
  static final int NCL_TOUCHED = 0x34;
  static final int FDL_TOUCHED = 0x35;

  static final int ELEPROX_MHD_RISING = 0x36;
  static final int ELEPROX_NHD_RISING = 0x37;
  static final int ELEPROX_NCL_RISING = 0x38;
  static final int ELEPROX_FDL_RISING = 0x39;

  static final int ELEPROX_MHD_FALLING = 0x3A;
  static final int ELEPROX_NHD_FALLING = 0x3B;
  static final int ELEPROX_NCL_FALLING = 0x3C;
  static final int ELEPROX_FDL_FALLING = 0x3D;

  static final int ELEPROX_NHD_TOUCHED = 0x3E;
  static final int ELEPROX_NCL_TOUCHED = 0x3F;
  static final int ELEPROX_FDL_TOUCHED = 0x40;

  static final int ELE0_TOUCH_THRESHOLD = 0x41;
  static final int ELE0_RELEASE_THRESHOLD = 0x42;
  static final int ELE1_TOUCH_THRESHOLD = 0x43;
  static final int ELE1_RELEASE_THRESHOLD = 0x44;
  static final int ELE2_TOUCH_THRESHOLD = 0x45;
  static final int ELE2_RELEASE_THRESHOLD = 0x46;
  static final int ELE3_TOUCH_THRESHOLD = 0x47;
  static final int ELE3_RELEASE_THRESHOLD = 0x48;
  static final int ELE4_TOUCH_THRESHOLD = 0x49;
  static final int ELE4_RELEASE_THRESHOLD = 0x4A;
  static final int ELE5_TOUCH_THRESHOLD = 0x4B;
  static final int ELE5_RELEASE_THRESHOLD = 0x4C;
  static final int ELE6_TOUCH_THRESHOLD = 0x4D;
  static final int ELE6_RELEASE_THRESHOLD = 0x4E;
  static final int ELE7_TOUCH_THRESHOLD = 0x4F;
  static final int ELE7_RELEASE_THRESHOLD = 0x50;
  static final int ELE8_TOUCH_THRESHOLD = 0x51;
  static final int ELE8_RELEASE_THRESHOLD = 0x52;
  static final int ELE9_TOUCH_THRESHOLD = 0x53;
  static final int ELE9_RELEASE_THRESHOLD = 0x54;
  static final int ELE10_TOUCH_THRESHOLD = 0x55;
  static final int ELE10_RELEASE_THRESHOLD = 0x56;
  static final int ELE11_TOUCH_THRESHOLD = 0x57;
  static final int ELE11_RELEASE_THRESHOLD = 0x58;
  static final int ELEPROX_TOUCH_THRESHOLD = 0x59;
  static final int ELEPROX_RELEASE_THRESHOLD = 0x5A;

  static final int DEBOUNCE_TOUCH_RELEASE = 0x5B;
  static final int AFE_CONFIGURATION_1 = 0x5C;
  static final int AFE_CONFIGURATION_2 = 0x5D;
  static final int ELECTRODE_CONFIGURAION_REGISTER = 0x5E;

  static final int ELE0_ELECTRODE_CURRENT = 0x5F;
  static final int ELE1_ELECTRODE_CURRENT = 0x60;
  static final int ELE2_ELECTRODE_CURRENT = 0x61;
  static final int ELE3_ELECTRODE_CURRENT = 0x62;
  static final int ELE4_ELECTRODE_CURRENT = 0x63;
  static final int ELE5_ELECTRODE_CURRENT = 0x64;
  static final int ELE6_ELECTRODE_CURRENT = 0x65;
  static final int ELE7_ELECTRODE_CURRENT = 0x66;
  static final int ELE8_ELECTRODE_CURRENT = 0x67;
  static final int ELE9_ELECTRODE_CURRENT = 0x68;
  static final int ELE10_ELECTRODE_CURRENT = 0x69;
  static final int ELE11_ELECTRODE_CURRENT = 0x6A;
  static final int ELEPROX_ELECTRODE_CURRENT = 0x6B;

  static final int ELE0_1_CHARGE_TIME = 0x6C;
  static final int ELE2_3_CHARGE_TIME = 0x6D;
  static final int ELE4_5_CHARGE_TIME = 0x6E;
  static final int ELE6_7_CHARGE_TIME = 0x6F;
  static final int ELE8_9_CHARGE_TIME = 0x70;
  static final int ELE10_11_CHARGE_TIME = 0x71;
  static final int ELEPROX_CHARGE_TIME = 0x72;

  static final int GPIO_CONTROL_REG_0 = 0x73;
  static final int GPIO_CONTROL_REG_1 = 0x74;
  static final int GPIO_DATA_REGISTER = 0x75;
  static final int GPIO_DIRECTION_REGISTER = 0x76;
  static final int GPIO_ENABLEREGISTER = 0x77;
  static final int GPIO_SET_REGISTER = 0x78;
  static final int GPIO_CLEAR_REGISTER = 0x79;
  static final int GPIO_TOGGLE_REGISTER = 0x7A;

  static final int AUTO_CONFIG_CONTROL_REG0 = 0x7B;
  static final int AUTO_CONFIG_CONTROL_REG1 = 0x7C;
  static final int AUTO_CONFIG_USL_REG = 0x7D;
  static final int AUTO_CONFIG_LSL_REG = 0x7E;
  static final int AUTO_CONFIG_TARGET_LEVEL_REG = 0x7F;

  static final int SOFT_RESET_REGISTER = 0x80;
  static final int SOFT_RESET = 0x63;

  int touchsensors = 12; // Number of pins used for touch sensing. The rest can
                         // be used as GPIO pins
  /**
   * pin named map of all the pins on the board
   */
  Map<String, PinDefinition> pinMap = null;
  /**
   * the definitive sequence of pins - "true address"
   */
  Map<Integer, PinDefinition> pinIndex = null;

  transient Map<String, PinArrayListener> pinArrayListeners = new HashMap<String, PinArrayListener>();

  /**
   * map of pin listeners
   */
  transient Map<Integer, List<PinListener>> pinListeners = new HashMap<Integer, List<PinListener>>();

  /**
   * the map of pins which the pin listeners are listening too - if the set is
   * null they are listening to "any" published pin
   */
  Map<String, Set<Integer>> pinSets = new HashMap<String, Set<Integer>>();

  double sampleFreq = 1; // Set
                         // default // hZ.

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);

    try {
      Mpr121 mpr121 = (Mpr121) Runtime.start("mpr121", "Mpr121");
      Runtime.start("gui", "SwingGui");
      Esp8266_01 esp = (Esp8266_01) Runtime.start("esp", "Esp8266_01");

      esp.setHost("esp8266-02.local");
      mpr121.attach("esp", "0", "0x5A");

      mpr121.begin();
      log.info(String.format("Reading touch sensor, %d", mpr121.touched()));

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Mpr121(String n) {
    super(n);
    createPinList();
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();

  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    return controllers;
  }

  public I2CController getController() {
    return controller;
  }

  public String getControllerName() {

    String controlerName = null;

    if (controller != null) {
      controlerName = controller.getName();
    }

    return controlerName;
  }

  @Override
  public void setDeviceBus(String deviceBus) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceBus = deviceBus;
    broadcastState();
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  /**
   * This method reads all touch controls
   */
  public void refresh() {

    broadcastState();
  }

  /**
   * Initiate the MPR121 to use all inputs for sensing
   * 
   * @return
   */
  public boolean begin() {

    if (!isAttached) {
      log.error("Must be attached to an i2c controller before using the begin method");
      return false;
    }

    int config = 0;
    int retries = 0;
    while (config != 0x24) {
      writeRegister(SOFT_RESET_REGISTER, SOFT_RESET);
      sleep(1);
      config = readRegister8(AFE_CONFIGURATION_2);
      log.info(String.format("AFE Configuraiton register 0x%02X", config));
      retries ++;
      if (retries > 10) break;
    }

    if (config != 0x24) {
      log.error("Unable to initiate the MPR121 i2c device");
      return false;
    }

    setStopMode();

    setThresholds(12, 6);

    writeRegister(MHD_RISING, 0x01);
    writeRegister(NHD_RISING, 0x01);
    writeRegister(NCL_RISING, 0x0E);
    writeRegister(FDL_RISING, 0x00);

    writeRegister(MHD_FALLING, 0x01);
    writeRegister(NHD_FALLING, 0x05);
    writeRegister(NCL_FALLING, 0x01);
    writeRegister(FDL_FALLING, 0x00);

    writeRegister(NHD_TOUCHED, 0x00);
    writeRegister(NCL_TOUCHED, 0x00);
    writeRegister(FDL_TOUCHED, 0x00);

    writeRegister(DEBOUNCE_TOUCH_RELEASE, 0);
    writeRegister(AFE_CONFIGURATION_1, 0x10); // default, 16uA charge current
    writeRegister(AFE_CONFIGURATION_2, 0x20); // 0.5uS encoding, 1ms period

    writeRegister(ELECTRODE_CONFIGURAION_REGISTER, 0x8F);

    return true;
  }

  void setThresholds(int touch, int release) {
    for (int i = 0; i < touchsensors; i++) {
      writeRegister(ELE0_TOUCH_THRESHOLD + 2 * i, touch);
      writeRegister(ELE0_RELEASE_THRESHOLD + 2 * i, release);

    }
  }

  int filteredData(int t) {
    if (t > touchsensors)
      return 0;
    return readRegister16(ELE0_FILTERED_DATA_LSB + t * 2);
  }

  int baselineData(int t) {
    if (t > touchsensors)
      return 0;
    int bl = readRegister8(ELE0_BASELINE_VALUE + t);
    return (bl << 2);
  }

  int touched() {
    int t = readRegister16(TOUCH_STATUS0);
    if ((t & 0x8000) == 0x8000) {
      log.error("Over current detected, resetting");
      writeRegister(TOUCH_STATUS1, 0x80);
      begin();
      return 0;
    }
    return t & 0x0FFF;
  }

  void getTouched() {
    int t = touched();
    for (int i = 0; i < touchsensors; i++) {
      pinIndex.get(i).setValue((t >> i) & 0x01);
    }
  }

  /**
   * This method starts the MPR121 measuring pins = Number of pins to use for
   * measuring starting with ELE0 as number 1 Setting pins = 0 will stop
   * measuring
   */
  public void setRunMode(int pins) {
    writeRegister(ELECTRODE_CONFIGURAION_REGISTER, pins + 1);
    touchsensors = pins;
  }

  /**
   * This method starts the MPR121 measuring pins = Number of pins to use for
   * measuring starting with ELE0 as number 1 Setting pins = 0 will stop
   * measuring
   */
  public void setStopMode() {
    setRunMode(0);
  }

  void i2cWrite(int reg) {
    if (!isAttached) {
      log.error("Must be attached to an i2c controller before writing");
      return;
    }
    byte[] writebuffer = { (byte) reg };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  void writeRegister(int reg, int value) {
    if (!isAttached) {
      log.error("Must be attached to an i2c controller before writing");
      return;
    }
    byte[] writebuffer = { (byte) reg, (byte) (value & 0xff) };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  int readRegister8(int reg) {
    if (!isAttached) {
      log.error("Must be attached to an i2c controller before reading");
      return 0;
    }
    byte[] writebuffer = { (byte) reg };
    byte[] readbuffer = new byte[1];
    controller.i2cWriteRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length, readbuffer, readbuffer.length);
    return ((int) (readbuffer[0] & 0xff));
  }

  int readRegister16(int reg) {
    if (!isAttached) {
      log.error("Must be attached to an i2c controller before reading");
      return 0;
    }
    byte[] writebuffer = { (byte) reg };
    byte[] readbuffer = new byte[2];
    controller.i2cWriteRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length, readbuffer, readbuffer.length);
    return ((int) readbuffer[0]) << 8 | (int) (readbuffer[1] & 0xff);
  }

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services could be
   * a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public List<PinDefinition> getPinList() {
    List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
    return list;
  }

  @Override
  public int read(Integer address) {
    return pinIndex.get(address).getValue();
  }

  @Override
  public int read(String pinName) {
    return read(pinNameToAddress(pinName));
  }

  @Override
  public void pinMode(Integer address, String mode) {
    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
    } else {
      log.error("Ads1115 only supports INPUT mode");
    }

  }

  @Override
  public void write(Integer address, Integer value) {
    log.error("Ads1115 only supports read, not write");

  }

  @Override
  public PinData publishPin(PinData pinData) {
    // caching last value
    pinIndex.get(pinData.address).setValue(pinData.value);
    return pinData;
  }

  /**
   * publish all read pin data in one array at once
   */
  @Override
  public PinData[] publishPinArray(PinData[] pinData) {
    return pinData;
  }

  public void attach(String listener, int pinAddress) {
    attach((PinListener) Runtime.getService(listener), pinAddress);
  }

  @Override
  public void attach(PinListener listener, Integer pinAddress) {
    String name = listener.getName();

    if (listener.isLocal()) {
      List<PinListener> list = null;
      if (pinListeners.containsKey(pinAddress)) {
        list = pinListeners.get(pinAddress);
      } else {
        list = new ArrayList<PinListener>();
      }
      list.add(listener);
      pinListeners.put(pinAddress, list);

    } else {
      // setup for pub sub
      // FIXME - there is an architectual problem here
      // locally it works - but remotely - outbox would need to know
      // specifics of
      // the data its sending
      addListener("publishPin", name, "onPin");
    }

  }

  @Override
  public void attach(PinArrayListener listener) {
    pinArrayListeners.put(listener.getName(), listener);

  }

  @Override
  public void enablePin(Integer address) {
    if (controller == null) {
      error("must be connected to enable pins");
      return;
    }

    log.info(String.format("enablePin %s", address));
    PinDefinition pin = pinIndex.get(address);
    pin.setEnabled(true);
    invoke("publishPinDefinition", pin);

    if (!isPublishing) {
      log.info(String.format("Starting a new publisher instance"));
      publisher = new Publisher(getName());
      publisher.start();
    }
  }

  @Override
  public void disablePin(Integer address) {
    if (controller == null) {
      log.error("Must be connected to disable pins");
      return;
    }
    PinDefinition pin = pinIndex.get(address);
    pin.setEnabled(false);
    invoke("publishPinDefinition", pin);
  }

  @Override
  public void disablePins() {
    for (int i = 0; i < pinDataCnt; i++) {
      disablePin(i);
    }
    if (isPublishing) {
      isPublishing = false;
    }
  }

  public Map<String, PinDefinition> createPinList() {
    pinMap = new HashMap<String, PinDefinition>();
    pinIndex = new HashMap<Integer, PinDefinition>();

    for (int i = 0; i < pinDataCnt; ++i) {
      PinDefinition pindef = new PinDefinition(getName(), i);
      String name = String.format("ELE%d", i);
      pindef.setRx(false);
      pindef.setTx(false);
      pindef.setAnalog(true);
      pindef.setPwm(false);
      pindef.setPinName(name);
      pindef.setAddress(i);
      pindef.setMode("INPUT");
      if (i > 3) {
        name = String.format("ELE%d/LED%d", i, i - 3);
        pindef.canWrite(true);
      } else {
        pindef.canWrite(false);
      }
      pinMap.put(name, pindef);
      pinIndex.put(i, pindef);
    }

    return pinMap;
  }

  public Integer pinNameToAddress(String pinName) {
    if (!pinMap.containsKey(pinName)) {
      error("no pin %s exists", pinName);
      return null;
    }
    return pinMap.get(pinName).getAddress();
  }

  /*
   * Set the sample rate in Hz, I.e the number of polls per second
   * 
   * @return the rate that was set.
   */
  public double setSampleRate(double rate) {
    if (rate < 0) {
      log.error(String.format("setSampleRate. Rate must be > 0. Ignored %s, returning to %s", rate, this.sampleFreq));
      return this.sampleFreq;
    }
    this.sampleFreq = rate;
    return rate;
  }

  /*
   * method to communicate changes in pinmode or state changes
   * 
   * @return the pin definition passed in. (used by invoke.)
   */
  public PinDefinition publishPinDefinition(PinDefinition pinDef) {
    return pinDef;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Mpr121.class);
    meta.addDescription("MPR121 Touch sensor & LED Driver");
    meta.addCategory("shield", "sensor", "i2c");
    meta.setSponsor("Mats");
    meta.setAvailable(false);
    return meta;
  }

  @Override
  // TODO Implement individula sample rates per pin
  public void enablePin(Integer address, Integer rate) {
    setSampleRate(rate);
    enablePin(address);
  }

  // This section contains all the new attach logic
  @Override
  public void attach(String service) throws Exception {
    attach((Attachable) Runtime.getService(service));
  }

  @Override
  public void attach(Attachable service) throws Exception {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      attachI2CController((I2CController) service);
      return;
    }
  }

  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
    }

    controllerName = controller.getName();
    log.info(String.format("%s attach %s", getName(), controllerName));

    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;

    attachI2CController(controller);
    isAttached = true;
    broadcastState();
  }

  public void attachI2CController(I2CController controller) {

    if (isAttached(controller))
      return;

    if (this.controllerName != controller.getName()) {
      log.error(String.format("Trying to attached to %s, but already attached to (%s)", controller.getName(), this.controllerName));
      return;
    }

    this.controller = controller;
    isAttached = true;
    controller.attachI2CControl(this);
    log.info(String.format("Attached %s device on bus: %s address %s", controllerName, deviceBus, deviceAddress));
    broadcastState();
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach((Attachable) Runtime.getService(service));
  }

  @Override
  public void detach(Attachable service) {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
      return;
    }
  }

  @Override
  public void detachI2CController(I2CController controller) {

    if (!isAttached(controller))
      return;

    controller.detachI2CControl(this);
    isAttached = false;
    broadcastState();
  }

  // This section contains all the methods used to query / show all attached
  // methods
  /**
   * Returns all the currently attached services
   */
  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null && isAttached) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public String getDeviceBus() {
    return this.deviceBus;
  }

  @Override
  public String getDeviceAddress() {
    return this.deviceAddress;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    ;
    return false;
  }
  
  public PinDefinition getPin(String pinName) {
    if (pinMap.containsKey(pinName)){
      return pinMap.get(pinName);
    }
    return null;
  }
  
  public PinDefinition getPin(Integer address) {
    if (pinIndex.containsKey(address)){
      return pinIndex.get(address);
    }
    return null;
  }
}
