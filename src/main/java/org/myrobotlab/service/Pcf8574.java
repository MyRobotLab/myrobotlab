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
 * References:
 * http://www.digikey.com/product-detail/en/nxp-semiconductors/PCF8574T-3,518/568-1077-1-ND/735791
 * 
 * A quasi-bidirectional I/O is an input or output port without using a
 * direction control register. Whenever the master reads the register, the value
 * returned to master depends on the actual voltage or status of the pin. At
 * power on, all the ports are HIGH with a weak 100 A internal pull-up to VDD,
 * but can be driven LOW by an internal transistor, or an external signal. The
 * I/O ports are entirely independent of each other, but each I/O octal is
 * controlled by the same read or write data byte.
 * 
 * @author Mats Onnerby modified by Ray Edgley.
 * 
 */

public class Pcf8574 extends Service<Pcf8574Config>
    implements I2CControl, /* FIXME - add I2CController */ PinArrayControl {
  /**
   * Publisher - Publishes pin data at a regular interval
   * 
   */
  public class Publisher extends Thread {

    public Publisher(String name) {
      super(String.format("%s.poller", name));
    }

    void publishPinData() {
      // read a single byte containing all 8 pins
      readRegister();
      List<PinData> pinArray = new ArrayList<>();
      for (int address = 0; address < pinDataCnt; ++address) {
        PinData pinData = new PinData(getPin(address).getPin(), getPin(address).getValue());
        PinDefinition pindef = getPin(address);
        
        if (pindef.isEnabled()) {
          invoke("publishPin", pinData);
        }
        pinArray.add(pinData);        
      }
      
      invoke("publishPinArray", new Object[] {pinArray.toArray(new PinData[0])});
    }

    @Override
    public void run() {
      Pcf8574Config c = (Pcf8574Config) config;
      log.info("New publisher instance started at a sample frequency of {} Hz", c.rateHz);
      long sleepTime = 1000 / (long) c.rateHz;
      isPolling = true;
      try {
        while (isPolling) {
          Thread.sleep(sleepTime);
          publishPinData();
        }

      } catch (Exception e) {
          isPolling = false;
          log.error("publisher threw", e);
      }
    }
  }

  public static final int INPUT = 0x0;

  public final static Logger log = LoggerFactory.getLogger(Pcf8574.class);

  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  // FIXME - remove this at some point ... publishing only needs name
  protected transient I2CController controller;

  protected List<String> controllers;

  /**
   * 0x20 - 0x27 for PCF8574 0c38 - 0x3F for PCF8574A Only difference between to
   * two IC circuits is the address range
   */
  protected List<String> deviceAddressList = Arrays.asList("0x20", "0x21", "0x22", "0x23", "0x24", "0x25", "0x26", "0x27", "0x38", "0x39", "0x3A", "0x3B", "0x3C", "0x3D", "0x3E",
      "0x3F", "0x49", "0x4A", "0x4B"); // Max9744

  protected List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");

  protected int directionRegister = 0xff; // byte

  protected boolean isAttached = false;

  protected boolean isPolling = false;

  protected transient Map<String, PinArrayListener> pinArrayListeners = new HashMap<String, PinArrayListener>();

  protected int pinDataCnt = 8;

  /**
   * the definitive sequence of pins - "true address"
   */
  protected Map<Integer, PinDefinition> pinIndex = new HashMap<>();

  /**
   * map of pin listeners FIXME - probably should be deprecated in favor of
   * simple mrl listener
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

  protected transient Thread polling = null;

  /**
   * The writeRegister is what was last sent to the PCF8574. By default on power
   * up, all pins are set to True.
   */
  protected int writeRegister = 0xFF; // byte

  public Pcf8574(String n, String id) {
    super(n, id);
    // registerForInterfaceChange(I2CController.class);
    createPinList();
    // refreshControllers();
    for (int i = 0; i < pinDataCnt; ++i) {
      int value = (writeRegister >> i) & 1;
      getPin(i).setValue(value);
    }
    subscribeToRuntime("registered");
  }

  @Override
  public void attach(Attachable attachable) throws Exception {

    if (I2CController.class.isAssignableFrom(attachable.getClass())) {
      attachI2CController((I2CController) attachable);
      return;
    }
  }

  @Override
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

  @Override
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
      attachI2CController((I2CController) si);
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

  @Override
  public void attachI2CController(I2CController controller) {
    Pcf8574Config c = (Pcf8574Config)config;
    
    if (c.controller == controller.getName()) {
      log.info("already attached to {}, use detach({}) first", c.controller, c.controller);
      return;
    }

    this.controller = controller;
    c.controller = controller.getName();
    isAttached = true;
    controller.attachI2CControl(this);
    log.info("attached {} device on bus: {} address {}", c.controller, c.bus, c.address);
    
    log.info("Starting a new publisher instance");
    polling = new Publisher(getName());
    polling.start();
    
    broadcastState();
  }

  public Map<String, PinDefinition> createPinList() {
    pinMap.clear();
    pinIndex.clear();

    for (int i = 0; i < pinDataCnt; ++i) {
      PinDefinition pindef = new PinDefinition(getName(), i);
      String name = String.format("P%d", i);
      pindef.setRx(false);
      pindef.setTx(false);
      pindef.setAnalog(false);
      pindef.setDigital(true);
      pindef.setPwm(false);
      pindef.setPinName(name);
      pindef.setAddress(i);
      pindef.setValue(1);
      pindef.setState(1);
      pindef.setMode("BIDIRECTIONAL");
      pinMap.put(name, pindef);
      pinIndex.put(i, pindef);
    }

    return pinMap;
  }

  @Override
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
    detach(Runtime.getService(service));
  }

  @Override
  public void detachI2CController(I2CController controller) {
    Pcf8574Config c = (Pcf8574Config)config;
    isPolling = false;    
    disablePins();
    
    if (!isAttached(controller))
      return;

    controller.detachI2CControl(this);
    c.controller = null;
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
    broadcastState();
  }

  @Override
  public void enablePin(int address, int rate) {
    setSampleRate(rate);
    enablePin(address);
  }

  @Override
  public void enablePin(String pin) {
    PinDefinition pindef = getPin(pin);
    if (pindef == null) {
      error("pin %s not found", pin);
      return;
    }
    enablePin(pindef.getAddress());
  }

  @Override
  public void enablePin(String pin, int rate) {
    PinDefinition pindef = getPin(pin);
    if (pindef == null) {
      error("pin %s not found", pin);
      return;
    }
    enablePin(pindef.getAddress(), rate);
  }

  @Override
  public String getAddress() {
    Pcf8574Config c = (Pcf8574Config)config;
    return c.address;
  }

  @Override
  public Integer getAddress(String pin) {
    PinDefinition pindef = getPin(pin);
    if (pindef == null) {
      error("pin %s not found", pin);
      return null;
    }
    return pindef.getAddress();
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
    Pcf8574Config c = (Pcf8574Config)config;
    return c.bus;
  }

  @Override
  @Deprecated /* use getAddress */
  public String getDeviceAddress() {
    Pcf8574Config c = (Pcf8574Config)config;
    return c.address;
  }

  @Override
  @Deprecated /* use getBus */
  public String getDeviceBus() {
    Pcf8574Config c = (Pcf8574Config)config;
    return c.bus;
  }

  @Override
  public PinDefinition getPin(int address) {
    if (pinIndex.containsKey(address)) {
      return pinIndex.get(address);
    }
    log.error("pinIndex does not contain address {}", address);
    return null;
  }

  @Override
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
    // There is no direction register in the PCF8574 it is always BIDRECTIONAL.
    pinDef.setMode("BIDIRECTIONAL");
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void pinMode(int address, String mode) {
    PinDefinition pinDef = getPin(address);
    // There is no direction register in the PCF8574 it is always BIDRECTIONAL.
    if (mode != "BIDIRECTIONAL") {
      log.error("There is no direction register, address {} mode must be BIDIRECTIONAL", address);
    }
    pinDef.setMode("BIDIRECTIONAL");
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void pinMode(String pin, String mode) {
    pinMode(getPin(pin).getAddress(), mode);
  }

  @Override
  public PinData publishPin(PinData pinData) {
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
  @Override
  public PinDefinition publishPinDefinition(PinDefinition pinDef) {
    return pinDef;
  }

  @Override
  public int read(int address) {
      readRegister();
    return getPin(address).getValue();
  }

  @Override
  public int read(String pinName) {
    return read(getPin(pinName).getAddress());
  }

  /**
   * Reads the input register from the PCF8574.
   * 
   * @return Register Value.
   */
  public int readRegister() {
    byte[] readbuffer = new byte[1];
    Pcf8574Config c = (Pcf8574Config)config;
    controller.i2cRead(this, Integer.parseInt(c.bus), Integer.decode(c.address), readbuffer, readbuffer.length);
    int dataread = (readbuffer[0]) & 0xff;
    for (int i = 0; i < 8; i++) {
      int value = (dataread >> i) & 1;
      getPin(i).setValue(value);
    }    
    return dataread;
  }

  /**
   * This returns the last set value on the pin. When this is set to True, a
   * read value of false indicates the pin is pulled low by external influence.
   * 
   * @param address
   *          Integer The pin to be looked at
   * @return current state of the output register for the pin
   */
  public int readOutputPin(int address) {
    int value = (writeRegister >> address) & 1;
    return value;
  }

  /**
   * This returns the last set value on the pin. When this is set to True, a
   * read value of false indicates the pin is pulled low by external influence.
   * 
   * @param pinName
   *          String
   * @return current state of the output register for the pin
   */
  public int readOutputPin(String pinName) {
    return readOutputPin(getPin(pinName).getAddress());
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

  /**
   * Set the I2C Address of the device.
   * 
   */
  @Override
  public void setDeviceAddress(String deviceAddress) {
    Pcf8574Config c = (Pcf8574Config)config;
    c.address = deviceAddress;
    broadcastState();
  }

  /**
   * Set the bus the device is physically attached to.
   * 
   */
  @Override
  public void setDeviceBus(String deviceBus) {
    Pcf8574Config c = (Pcf8574Config)config;
    c.bus = deviceBus;
    broadcastState();
  }

  /**
   * Set the sample rate in Hz, I.e the number of polls per second
   * 
   * @return - returns the rate that was set
   */
  public double setSampleRate(double rate) {
    Pcf8574Config c = (Pcf8574Config) config;
    if (rate < 0) {
      log.error("setSampleRate. Rate must be > 0. Ignored {}, returning to {}", rate, c.rateHz);
      return c.rateHz;
    }
    c.rateHz = rate;
    return rate;
  }

  @Override
  public void write(int address, int value) {
    log.info("Write Pin int {} with {}", address, value);
    // PinDefinition pinDef = getPin(address); // this doesn't get used at all
    if (value == 0) {
      writeRegister = writeRegister &= ~(1 << address);
    } else {
      writeRegister = writeRegister |= (1 << address);
    }
    writeRegister(writeRegister);
    // The writeRegister and the value we read in are not the same thing.
    // We should not be setting the pins value based on what we write out to it.
    // pinDef.setValue(value);
  }

  @Override
  public void write(String pin, int value) {
    // log.info("Write Pin string {} with {}", pin, value);
    if (getPin(pin) == null) {
      error("could not get pin %s", pin);
      return;
    }
    write(getPin(pin).getAddress(), value);
  }

  public void writeRegister(int data) {
    Pcf8574Config c = (Pcf8574Config) config;
    byte[] writebuffer = { (byte) data };
    controller.i2cWrite(this, Integer.parseInt(c.bus), Integer.decode(c.address), writebuffer, writebuffer.length);
    writeRegister = data; // good idea to save the current state of the output
                          // register when we write out the register to the
                          // PCF8574.
    for (int i = 0; i < pinDataCnt; ++i) {
      int value = (data >> i) & 1;
      getPin(i).setState(value);
    }
    readRegister();
    broadcastState();
  }

  public static void main(String[] args) {
    LoggingFactory.init("Info");

    try {

      Runtime.start("mega", "Arduino");
      Runtime.start("webgui", "WebGui");
      Runtime.start("pcf", "Pcf8574");
      // arduino = Runtime.start("arduino","Arduino")
      // arduino.setBoardMega()
      // arduino.connect("COM3")

      // int KeyColumn = 0;
      // int LastKeyPress = 0;
      // Pcf8574 KeyPad = (Pcf8574) Runtime.start("pcf", "Pcf8574");
      // Before we can use this,
      // we need to configure the I2C Bus
      // KeyPad.setBus("1")
      // and address then connect it.
      // KeyPad.setAddress("0x20")
      // KeyPad.attachI2CController(raspi)
      // KeyPad.attach(raspi, "1", "0x20");
      // KeyPad.attach(raspi, "1", "0x20");
      // KeyPad.attach(raspi);
      // KeyPad.setBus("1");
      // KeyPad.setAddress("0x20");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  @Override
  public void stopService() {
    super.stopService();
    isPolling = false;
  }

  @Override
  public Pcf8574Config getConfig() {
    super.getConfig();
    return config;
  }

  @Override
  public Pcf8574Config apply(Pcf8574Config c) {
    super.apply(c);
    // FIXME remove local fields in favor of config only
    if (c.address != null) {
      setAddress(c.address);
    }
    if (c.bus != null) {
      setBus(c.bus);
    }

    if (c.controller != null) {
      try {
        attach(c.controller);
      } catch (Exception e) {
        error(e);
      }
    }
    return c;
  }

}
