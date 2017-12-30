package org.myrobotlab.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.VoltageSensorControl;
import org.slf4j.Logger;

//import com.pi4j.io.i2c.I2CBus;
/**
 * AdaFruit Ina219 Shield Controller Service
 * 
 * @author Mats
 * 
 *         References : https://www.adafruit.com/products/904
 */
public class AdafruitIna219 extends Service implements I2CControl, VoltageSensorControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AdafruitIna219.class);
  transient public I2CController controller;

  public static final byte INA219_SHUNTVOLTAGE = 0x01;
  public static final byte INA219_BUSVOLTAGE = 0x02;

  public List<String> deviceAddressList = Arrays.asList("0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46", "0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E",
      "0x4F");

  public String deviceAddress = "0x40";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public int busVoltage = 0;
  public double shuntVoltage = 0;
  public double current = 0.0;
  public double power = 0.0;

  // TODO Add methods to calibrate
  // Currently only supports setting the shunt resistance to a different
  // value than the default, in case it has been exchanged to measure
  // a different range of current
  public double shuntResistance = 0.1; // expressed in Ohms
  public int scaleRange = 32; // 32V = bus full-scale range
  public int pga = 8; // 320 mV = shunt full-scale range

  public List<String> controllers;
  public String controllerName;

  public boolean isAttached = false;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      AdafruitIna219 adafruitINA219 = (AdafruitIna219) Runtime.start("AdafruitIna219", "AdafruitIna219");
      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");

      byte msb = (byte) 0x83;
      byte lsb = (byte) 0x00;
      double test = (double) ((((int) msb) << 8 | (int) lsb & 0xff)) * .01;
      log.info(String.format("msb = %s, lsb = %s, test = %s", msb, lsb, test));
      // (((int)(readbuffer[0] & 0xff) << 5)) | ((int)(readbuffer[1] >>
      // 3));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AdafruitIna219(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();

  }

  /*
   * Refresh the list of running services that can be selected in the GUI
   */
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

  /**
   * This method sets the shunt resistance in ohms Default value is .1 Ohms (
   * R100 )
   */
  // @Override
  public void setShuntResistance(double shuntResistance) {
    this.shuntResistance = shuntResistance;
  }

  // @Override
  public double getShuntResistance() {
    return shuntResistance;
  }

  /**
   * This method reads and returns the power in milliWatts
   */
  public void refresh() {

    power = getPower();
    broadcastState();
  }

  // @Override
  public double getPower() {
    power = getBusVoltage() * getCurrent() / 1000;
    return power;
  }

  /**
   * This method reads and returns the shunt current in milliAmperes
   */
  // @Override
  public double getCurrent() {
    current = getShuntVoltage() / shuntResistance;
    return current;
  }

  /**
   * This method reads and returns the shunt Voltage in milliVolts
   */
  // @Override
  public double getShuntVoltage() {
    byte[] writebuffer = { INA219_SHUNTVOLTAGE };
    byte[] readbuffer = { 0x0, 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // log.info(String.format("getShuntVoltage x%02X x%02X", readbuffer[0],
    // readbuffer[1]));
    // The shuntVoltage is signed so the MSB can have sign bits, that needs
    // to remain
    shuntVoltage = (double) ((((int) readbuffer[0]) << 8 | (int) readbuffer[1] & 0xff)) * .01;
    return shuntVoltage;
  }

  /**
   * This method reads and returns the bus Voltage in milliVolts
   */
  // @Override
  public double getBusVoltage() {
    byte[] writebuffer = { INA219_BUSVOLTAGE };
    byte[] readbuffer = { 0x0, 0x0 };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    // A bit tricky conversion. The LSB needs to be right shifted 3 bits, so
    // the MSB needs to be left shifted (8-3) = 5 bits
    // And bytes are signed in Java so first a mask of 0xff needs to be
    // applied to the MSB to remove the sign
    int rawBusVoltage = (((int) readbuffer[0] & 0xff) << 8 | (int) readbuffer[1] & 0xff) >> 3;
    log.debug(String.format("Busvoltage high byte = %s, low byte = %s, rawBusVoltagee = %s", readbuffer[0], readbuffer[1], rawBusVoltage));
    // LSB = 4mV, so multiply wit 4 to get the volatage in mV
    busVoltage = rawBusVoltage * 4;
    return busVoltage;
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

    ServiceType meta = new ServiceType(AdafruitIna219.class.getCanonicalName());
    meta.addDescription("measures voltage and current of a circuit");
    meta.setLicenseApache();
    meta.addCategory("shield", "sensor", "i2c");
    meta.setSponsor("Mats");
    return meta;
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
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName, controller.getName()));
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
}
