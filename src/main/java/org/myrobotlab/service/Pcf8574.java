package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.Pcf8574Config;
import org.myrobotlab.service.config.ServiceConfig;
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
 *         FIXME - this is a i2c controller as well as an i2c device
 * 
 */

public class Pcf8574 extends Service
    implements I2CControl, /* FIXME - add I2CController */ PinArrayControl {
  /**
   * Publisher - Publishes pin data at a regular interval
   * 
   */
  public class Publisher extends Thread {

    public Publisher(String name) {
      super(String.format("%s.publisher", name));
    }

    // FIXME - have a read "rate" in Hz
    void publishPinData() {
      // Read a single byte containing all 8 pins
      read8();
      PinData[] pinArray = new PinData[pinDataCnt];
      for (int i = 0; i < pinArray.length; ++i) {
        PinData pinData = new PinData(i, read(i));
        pinArray[i] = pinData;
        String address = pinData.pin;

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

    // FIXME - have a read "rate" in Hz
    @Override
    public void run() {

      log.info("New publisher instance started at a sample frequency of {} Hz", sampleFreq);
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
  }

  public static final int INPUT = 0x0;
  
  public final static Logger log = LoggerFactory.getLogger(Pcf8574.class);

  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  protected transient I2CController controller;

  protected String controllerName;
  
  protected List<String> controllers;

  protected String deviceAddress = "0x38";
  
  /*
   * 0x20 - 0x27 for PCF8574 0c38 - 0x3F for PCF8574A Only difference between to
   * two IC circuits is the address range
   */
  protected List<String> deviceAddressList = Arrays.asList("0x20", "0x21", "0x22", "0x23", "0x24", "0x25", "0x26", "0x27", "0x38", "0x39", "0x3A", "0x3B", "0x3C", "0x3D", "0x3E",
      "0x3F", "0x49", "0x4A", "0x4B"); // Max9744

  protected String deviceBus = "1";

  protected List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");

  protected int directionRegister = 0xff; // byte

  protected boolean isAttached = false;
  
  // Publisher
  protected boolean isPublishing = false;

  protected transient Map<String, PinArrayListener> pinArrayListeners = new HashMap<String, PinArrayListener>();

  protected int pinDataCnt = 8;

  /**
   * the definitive sequence of pins - "true address"
   */
  protected Map<Integer, PinDefinition> pinIndex = new HashMap<>();

  /**
   * map of pin listeners
   */
  protected transient Map<String, List<PinListener>> pinListeners = new HashMap<String, List<PinListener>>();

  /**
   * pin named map of all the pins on the board
   */
  protected Map<String, PinDefinition> pinMap = new TreeMap<>();
  /**
   * the map of pins which the pin listeners are listening too - if the set is
   * null they are listening to "any" published pin
   */
  protected Map<String, Set<Integer>> pinSets = new HashMap<String, Set<Integer>>();

  protected transient Thread publisher = null;

  /**
   * default sample rate 1 Hz
   */
  protected double sampleFreq = 1; // Set

  // track
  // of
  // I/O
  // for
  // each
  // pin
  protected int writeRegister = 0; // byte
  // to
  // write
  // after
  // taking
  // care
  // of
  // input
  // output
  // assignment

  public Pcf8574(String n, String id) {
    super(n, id);
    registerForInterfaceChange(I2CController.class);
    createPinList();
    refreshControllers();
    subscribeToRuntime("registered");
  }

  @Override
  public void attach(Attachable attachable) throws Exception {

    if (I2CController.class.isAssignableFrom(attachable.getClass())) {
      attachI2CController((I2CController) attachable);
      return;
    }
  }

  @Deprecated /* use attach(controller) */
  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (deviceBus != null) {
      setBus(deviceBus);
    }

    if (deviceAddress != null) {
      setAddress(deviceAddress);
    }

    attachI2CController(controller);
  }

  @Override
  public void attachPinArrayListener(PinArrayListener listener) {
    pinArrayListeners.put(listener.getName(), listener);

  }

  @Override
  public void attachPinListener(PinListener listener, int address) {
    attach(listener, String.format("%d", address));
  }

  public void attach(PinListener listener, String pin) {
    String name = listener.getName();

    if (listener.isLocal()) {
      List<PinListener> list = null;
      if (pinListeners.containsKey(pin)) {
        list = pinListeners.get(pin);
      } else {
        list = new ArrayList<PinListener>();
      }
      list.add(listener);
      pinListeners.put(pin, list);

    } else {
      // setup for pub sub
      // FIXME - there is an architectual problem here
      // locally it works - but remotely - outbox would need to know
      // specifics of
      // the data its sending
      addListener("publishPin", name, "onPin");
    }

  }

  // This section contains all the new attach logic
  @Override
  public void attach(String name) throws Exception {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof I2CController) {
      attachI2CController((I2CController)si);
      return;
    } else {
      log.error("%s does not know how to attach to %s of type %s", getName(), si.getName(), si.getSimpleName());
    }
  }

  @Deprecated /* use attach(String) */
  public void attach(String listener, int pinAddress) {
    attachPinListener((PinListener) Runtime.getService(listener), pinAddress);
  }

  @Deprecated /* use attach(String) */
  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public void attachI2CController(I2CController controller) {

    if (this.controllerName == controller.getName()) {
      log.info("already attached to {}, use detach({}) first", controllerName, controllerName);
      return;
    }

    this.controller = controller;
    controllerName = controller.getName();
    isAttached = true;
    controller.attachI2CControl(this);
    log.info("attached {} device on bus: {} address {}", controllerName, deviceBus, deviceAddress);
    broadcastState();
  }

  public Map<String, PinDefinition> createPinList() {
    pinMap.clear();
    pinIndex.clear();

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
  
  public void detach() {
    if (controller != null) {
      detachI2CController(controller);
    }
  }

  @Override
  public void detach(Attachable service) {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
      return;
    }
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach((Attachable) Runtime.getService(service));
  }

  @Override
  public void detachI2CController(I2CController controller) {

    if (!isAttached(controller))
      return;

    controller.detachI2CControl(this);
    controllerName = null;
    isAttached = false;
    broadcastState();
  }

  @Override
  public void disablePin(int address) {
    if (controller == null) {
      log.error("Must be connected to disable pins");
      return;
    }
    PinDefinition pin = getPin(address);
    pin.setEnabled(false);
    invoke("publishPinDefinition", pin);
  }

  @Override
  public void disablePin(String pin) {
    disablePin(getPin(pin).getAddress());
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

  @Override
  public void enablePin(int address) {
    if (controller == null) {
      error("must be connected to enable pins");
      return;
    }

    log.info("enablePin {}", address);
    PinDefinition pin = getPin(address);
    pin.setEnabled(true);
    invoke("publishPinDefinition", pin);

    if (!isPublishing) {
      log.info("Starting a new publisher instance");
      publisher = new Publisher(getName());
      publisher.start();
    }
  }

  @Override
  public void enablePin(int address, int rate) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enablePin(String pin) {
    enablePin(getPin(pin).getAddress());
  }

  @Override
  public void enablePin(String pin, int rate) {
    enablePin(getPin(pin).getAddress(), rate);
  }

  @Override
  public String getAddress() {
    return deviceAddress;
  }

  @Override
  public Integer getAddress(String pin) {
    // TODO Auto-generated method stub
    return null;
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
  public String getBus() {
    return deviceBus;
  }

  @Override
  public String getDeviceAddress() {
    return this.deviceAddress;
  }

  @Override
  public String getDeviceBus() {
    return this.deviceBus;
  }

  @Override
  public PinDefinition getPin(int address) {
    if (pinIndex.containsKey(address)) {
      return pinIndex.get(address);
    }
    log.error("pinIndex does not contain address {}", address);
    return null;
  }

  public PinDefinition getPin(String pin) {
    if (pinMap.containsKey(pin)) {
      return pinMap.get(pin);
    }
    log.error("pinMap does not contain pin {}", pin);
    return null;
  }

  @Override
  public List<PinDefinition> getPinList() {
    List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
    return list;
  }

  public boolean isAttached() {
    return isAttached;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    boolean ret = false;
    try {
      ret = isAttached(Runtime.getService(name));
    } catch (Exception e) {
    }
    return ret;
  }

  public void onRegistered(Registration s) {
    refreshControllers();
    broadcastState();

  }

  public void pinMode(int address, int mode) {
    PinDefinition pinDef = getPin(address);
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
  public void pinMode(int address, String mode) {
    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
      pinMode(address, INPUT);
    } else {
      pinMode(address, OUTPUT);
    }
  }

  @Override
  public void pinMode(String pin, String mode) {
    pinMode(getPin(pin).getAddress(), mode);
  }

  @Override
  public PinData publishPin(PinData pinData) {
    // caching last value
    getPin(pinData.pin).setValue(pinData.value);
    return pinData;
  }

  /**
   * publish all read pin data in one array at once
   */
  @Override
  public PinData[] publishPinArray(PinData[] pinData) {
    return pinData;
  }

  /*
   * method to communicate changes in pinmode or state changes
   * 
   */
  public PinDefinition publishPinDefinition(PinDefinition pinDef) {
    return pinDef;
  }

  @Override
  public int read(int address) {
    // When publishing the refresh is done in the publishing method
    // otherwise refresh the pinarray
    if (!isPublishing)
      read8();
    return getPin(address).getValue();
  }

  @Override
  public int read(String pinName) {
    return read(getPin(pinName).getAddress());
  }

  int read8() {
    int dataread = readRegister();
    for (int i = 0; i < 8; i++) {
      int value = (dataread >> i) & 1;
      getPin(i).setValue(value);
    }
    return dataread;
  }

  int readRegister() {
    byte[] readbuffer = new byte[1];
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    return ((int) readbuffer[0]) & 0xff;
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    return controllers;
  }

  @Override
  public void setAddress(String address) {
    setDeviceAddress(address);
  }

  @Override
  public void setBus(String bus) {
    setDeviceBus(bus);
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  @Override
  public void setDeviceBus(String deviceBus) {
    this.deviceBus = deviceBus;
    broadcastState();
  }

  /*
   * Set the sample rate in Hz, I.e the number of polls per second
   * 
   * @return - returns the rate that was set
   */
  public double setSampleRate(double rate) {
    if (rate < 0) {
      log.error("setSampleRate. Rate must be > 0. Ignored {}, returning to {}", rate, this.sampleFreq);
      return this.sampleFreq;
    }
    this.sampleFreq = rate;
    return rate;
  }

  @Override
  public void write(int address, int value) {

    PinDefinition pinDef = getPin(address);
    if (pinDef.getMode() == "OUTPUT") {
      if (value == 0) {
        writeRegister = directionRegister &= ~(1 << address);
      } else {
        writeRegister = directionRegister |= (1 << address);
      }
    } else {
      log.error("Can't write to a pin in input mode. Change direction to OUTPUT ({}) with pinMode first.", OUTPUT);
    }
    writeRegister(writeRegister);
    pinDef.setValue(value);
  }

  @Override
  public void write(String pin, int value) {
    write(getPin(pin).getAddress(), value);
  }

  public void writeRegister(int data) {
    byte[] writebuffer = { (byte) data };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }
  
  public static void main(String[] args) {
    LoggingFactory.init("info");

    try {

      Runtime.start("mega", "Arduino");
      Runtime.start("webgui", "WebGui");
      
      Runtime.start("lcd", "Hd44780");
      // arduino = Runtime.start("arduino","Arduino")
      // arduino.setBoardMega()
      // arduino.connect("COM3")
      
      int KeyColumn = 0;
      int LastKeyPress = 0;
      Pcf8574 KeyPad = (Pcf8574) Runtime.start("pcf", "Pcf8574");
      // Before we can use this,
      // we need to configure the I2C Bus
      // KeyPad.setBus("1")
      // and address then connect it.
      // KeyPad.setAddress("0x20")
      // KeyPad.attachI2CController(raspi)
      // KeyPad.attach(raspi, "1", "0x20");
      // KeyPad.attach(raspi, "1", "0x20");
//      KeyPad.attach(raspi);
//      KeyPad.setBus("1");
//      KeyPad.setAddress("0x20");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  @Override
  public ServiceConfig getConfig() {

    Pcf8574Config config = new Pcf8574Config();
    config.address = deviceAddress;
    config.bus = deviceBus;
    if (controller != null) {      
      config.controller = controllerName;
    }

    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    Pcf8574Config config = (Pcf8574Config) c;
    if (config.address != null) {
      setAddress(config.address);
    }
    if (config.bus != null) {
      setBus(config.bus);
    }
    
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch(Exception e) {
        error(e);
      }
    }
    return c;
  }
  

}
