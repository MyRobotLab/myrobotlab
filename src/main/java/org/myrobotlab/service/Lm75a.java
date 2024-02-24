package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.Lm75aConfig;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

/**
 * Lm75a Digital temperature sensor and thermal watchdog
 * 
 * @author Mats
 * 
 *         References : https://www.nxp.com/documents/data_sheet/LM75A.pdf
 */
public class Lm75a extends Service<Lm75aConfig> implements I2CControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Lm75a.class);
  public transient I2CController controller;

  // Register list
  public static final byte LM75A_CONF = 0x01; //
  public static final byte LM75A_TEMP = 0x00;
  public static final byte LM75A_TOS = 0x03;
  public static final byte LM75A_THYST = 0x02;

  public List<String> deviceAddressList = Arrays.asList("0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E", "0x4F");

  public String deviceAddress = "0x48";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public double temperature = 0;

  public List<String> controllers;
  public String controllerName;

  public boolean isAttached = false;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      Lm75a lm75a = (Lm75a) Runtime.start("tempsensor", "Lm75a");
      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");
      /*
       * byte msb = (byte) 0x83; byte lsb = (byte) 0x00; double test = (double)
       * ((((int) msb) << 8 | (int) lsb & 0xff)) * .01;
       * log.info(String.format("msb = %s, lsb = %s, test = %s", msb, lsb,
       * test));
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Lm75a(String n, String id) {
    super(n, id);
    refreshControllers();
    subscribeToRuntime("registered");
  }

  public void onRegistered(Registration s) {
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
      log.error("Already attached to {}, use detach({}) first", this.controllerName);
      return;
    }
    this.deviceBus = deviceBus;
    broadcastState();
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    if (isAttached) {
      log.error("Already attached to {}, use detach({}) first", this.controllerName);
      return;
    }
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  public void refresh() {
    getTemperature();
  }

  /**
   * This method reads and returns the raw temperature as two bytes where the
   * MSB is the integer part of the temperature ( + sign ) and the LSB is the
   * decimals. Used in two complement form.
   * 
   * @return - temperature in celsius .. i think .
   */
  public double getTemperature() {
    byte[] writebuffer = { LM75A_TEMP };
    byte[] readbuffer = { 0x0, 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // log.info(String.format("getTemperature 0x%02X 0x%02X", readbuffer[0],
    // readbuffer[1]));
    // The temperature is signed so the MSB can have sign bits, that needs
    // to remain
    int rawTemp = (readbuffer[0]) << 8 | readbuffer[1] & 0xff;
    temperature = (double) rawTemp / 256;
    broadcastState();
    return temperature;
  }

  public int getI2cConfig() {
    byte[] writebuffer = { LM75A_CONF };
    byte[] readbuffer = { 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // log.info(String.format("getConf 0x%02X", readbuffer[0]));
    // The temperature is signed so the MSB can have sign bits, that needs
    // to remain
    int config = readbuffer[0] & 0xff;
    return config;
  }

  public double getTos() {
    byte[] writebuffer = { LM75A_TOS };
    byte[] readbuffer = { 0x0, 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // log.info(String.format("getTos 0x%02X 0x%02X", readbuffer[0],
    // readbuffer[1]));
    // The temperature is signed so the MSB can have sign bits, that needs
    // to remain
    int rawTos = (readbuffer[0]) << 8 | readbuffer[1] & 0xff;
    double tos = rawTos / 256;
    return tos;
  }

  public void setTos(int tos) {
    byte[] writebuffer = { LM75A_TOS, (byte) tos, 0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  public double getThyst() {
    byte[] writebuffer = { LM75A_THYST };
    byte[] readbuffer = { 0x0, 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // log.info(String.format("getThyst 0x%02X 0x%02X", readbuffer[0],
    // readbuffer[1]));
    // The temperature is signed so the MSB can have sign bits, that needs
    // to remain
    int rawThyst = (readbuffer[0]) << 8 | readbuffer[1] & 0xff;
    double thyst = rawThyst / 256;
    return thyst;
  }

  public void setThyst(int thyst) {
    byte[] writebuffer = { LM75A_THYST, (byte) thyst, 0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  /**
   * valid for a control or controller which can only have a single other
   * service attached
   * 
   * @return true if the controller is attached. false otherwise
   */
  public boolean isAttached() {
    return isAttached;
  }

  // This section contains all the new attach logic
  @Override
  public void attach(String service) throws Exception {
    attach(Runtime.getService(service));
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

  @Override
  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error("Already attached to {}, use detach({}) first", this.controllerName);
    }

    controllerName = controller.getName();
    log.info("{} attach {}", getName(), controllerName);

    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;

    attachI2CController(controller);
    isAttached = true;
    broadcastState();
  }

  @Override
  public void attachI2CController(I2CController controller) {

    if (isAttached(controller))
      return;

    if (this.controllerName != controller.getName()) {
      log.error("Trying to attached to {}, but already attached to ({})", controller.getName(), this.controllerName);
      return;
    }

    this.controller = controller;
    isAttached = true;
    controller.attachI2CControl(this);
    log.info("Attached {} device on bus: {} address {}", controllerName, deviceBus, deviceAddress);
    broadcastState();
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach(Runtime.getService(service));
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
    return false;
  }

  @Override
  public void setBus(String bus) {
    setDeviceBus(bus);
  }

  @Override
  public void setAddress(String address) {
    setDeviceAddress(address);
  }

  @Override
  public String getBus() {
    return deviceBus;
  }

  @Override
  public String getAddress() {
    return deviceAddress;
  }

}
