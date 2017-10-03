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
 * PCF8574 / PCF8574A Remote I/O expander for i2c bus with interrupt ( interrupt
 * not yet implemented )
 * 
 * @author Mats
 * 
 *         References:
 *         http://www.digikey.com/product-detail/en/nxp-semiconductors
 *         /PCF8574T-3,518/568-1077-1-ND/735791
 * 
 */

public class Pcf8574 extends Service implements I2CControl, PinArrayControl {
  /**
   * Publisher - Publishes pin data at a regular interval
   * 
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
          log.error("publisher threw", e);
        }
      }
    }

    void publishPinData() {
      // Read a single byte containing all 8 pins
      read8();
      PinData[] pinArray = new PinData[pinDataCnt];
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
  transient Thread publisher = null;
  int pinDataCnt = 8;
  //

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Pcf8574.class);
  public transient I2CController controller;

  /*
   * 0x20 - 0x27 for PCF8574 0c38 - 0x3F for PCF8574A Only difference between to
   * two IC circuits is the address range
   */
  public List<String> deviceAddressList = Arrays.asList("0x20", "0x21", "0x22", "0x23", "0x24", "0x25", "0x26", "0x27", "0x38", "0x39", "0x3A", "0x3B", "0x3C", "0x3D", "0x3E",
      "0x3F", "0x49", "0x4A", "0x4B"); // Max9744
  // Addresses

  public String deviceAddress = "0x38";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public List<String> controllers;
  public String controllerName;
  public boolean isAttached = false;

  public static final int INPUT = 0x0;
  public static final int OUTPUT = 0x1;

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
  // default
  // sample
  // rate
  // to
  // 1
  // Hz
  // //
  // Sample
  // rate
  // in
  // hZ.

  int directionRegister = 0xff; // byte
  // track
  // of
  // I/O
  // for
  // each
  // pin
  int writeRegister = 0; // byte
  // to
  // write
  // after
  // taking
  // care
  // of
  // input
  // output
  // assignment

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);

    try {
      Pcf8574 pcf8574t = (Pcf8574) Runtime.start("Pcf8574t", "Pcf8574t");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Pcf8574(String n) {
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

  public void writeRegister(int data) {
    byte[] writebuffer = { (byte) data };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  int readRegister() {
    byte[] readbuffer = new byte[1];
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    return ((int) readbuffer[0]) & 0xff;
  }

  public boolean isAttached() {
    return isAttached;
  }

  @Override
  public List<PinDefinition> getPinList() {
    List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
    return list;
  }

  int read8() {
    int dataread = readRegister();
    for (int i = 0; i < 8; i++) {
      int value = (dataread >> i) & 1;
      pinIndex.get(i).setValue(value);
    }
    return dataread;
  }

  @Override
  public int read(Integer address) {
    // When publishing the refresh is done in the publishing method
    // otherwise refresh the pinarray
    if (!isPublishing)
      read8();
    return pinIndex.get(address).getValue();
  }

  @Override
  public int read(String pinName) {
    return read(pinNameToAddress(pinName));
  }

  @Override
  public void pinMode(Integer address, String mode) {
    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
      pinMode(address, INPUT);
    } else {
      pinMode(address, OUTPUT);
    }
  }

  public void pinMode(int address, int mode) {
    PinDefinition pinDef = pinIndex.get(address);
    if (mode == INPUT) {
      pinDef.setMode("INPUT");
      directionRegister &= ~(1 << address);
    } else {
      pinDef.setMode("OUTPUT");
      directionRegister |= (1 << address);
    }
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void write(Integer address, Integer value) {

    PinDefinition pinDef = pinIndex.get(address);
    if (pinDef.getMode() == "OUTPUT") {
      if (value == 0) {
        writeRegister = directionRegister &= ~(1 << address);
      } else {
        writeRegister = directionRegister |= (1 << address);
      }
    } else {
      log.error(String.format("Can't write to a pin in input mode. Change direction to OUTPUT (%s) with pinMode first.", OUTPUT));
    }
    writeRegister(writeRegister);
    pinDef.setValue(value);
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
      String name = String.format("D%d", i);
      pindef.setRx(false);
      pindef.setTx(false);
      pindef.setAnalog(false);
      pindef.setDigital(true);
      pindef.setPwm(false);
      pindef.setPinName(name);
      pindef.setAddress(i);
      pindef.setMode("INPUT");
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
   * @return - returns the rate that was set
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

    ServiceType meta = new ServiceType(Pcf8574.class.getCanonicalName());
    meta.addDescription("Pcf8574 i2c 8 pin I/O extender");
    meta.addCategory("shield", "sensor");
    meta.setSponsor("Mats");
    return meta;
  }

  @Override
  public void enablePin(Integer address, Integer rate) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isAttached(String name) {
    // TODO Auto-generated method stub
    return false;
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
