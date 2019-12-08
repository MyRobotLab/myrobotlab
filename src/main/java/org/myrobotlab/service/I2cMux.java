package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

/**
 * 
 * I2CMux - This is the MyRobotLab Service that can be used if you have several
 * i2c devices that share the same address. Create one I2CMux for each of the
 * i2c buses that you want to use. It can be used with tca9548a and possibly
 * other devices.
 * 
 * 
 * @author Mats Onnerby
 * 
 *         More Info : https://www.adafruit.com/product/2717
 * 
 */
public class I2cMux extends Service implements I2CControl, I2CController {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(I2cMux.class.getCanonicalName());

  public transient I2CController controller;

  public List<String> controllers = new ArrayList<String>();
  public String controllerName;

  public List<String> deviceAddressList = Arrays.asList("0x70", "0x71", "0x72", "0x73", "0x74", "0x75", "0x76", "0x77");

  public String deviceAddress = "0x70";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public boolean isAttached = false;
  private int lastBusAddress = -1;

  public static class I2CDeviceMap {
    public String serviceName;
    public String busAddress;
    public String deviceAddress;
  }

  public HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  public static void main(String[] args) {
    LoggingFactory.init("info");

    try {
      I2cMux i2cMux = (I2cMux) Runtime.start("i2cMux", "I2CMux");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public I2cMux(String n, String id) {
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
    controllers.remove(this.getName());
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

  public boolean isAttached() {
    return isAttached;
  }

  public void setMuxBus(int busAddress) {
    if (busAddress != lastBusAddress) {
      byte bus[] = new byte[1];
      bus[0] = (byte) (1 << busAddress);
      log.debug("setMux this.deviceBus {} this.deviceAddress {} bus[0] {}", this.deviceBus, this.deviceAddress, bus[0]);
      controller.i2cWrite(this, Integer.parseInt(this.deviceBus), Integer.decode(this.deviceAddress), bus, bus.length);
      lastBusAddress = busAddress;
    }
  }

  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    setMuxBus(busAddress);
    String key = String.format("%d.%d", busAddress, deviceAddress);
    log.debug(String.format("i2cWrite busAddress x%02X deviceAddress x%02X key %s", busAddress, deviceAddress, key));
    controller.i2cWrite(this, Integer.parseInt(this.deviceBus), deviceAddress, buffer, size);
  }

  /**
   * TODO Add demuxing. i.e the route back to the caller The i2c will receive
   * data that neeeds to be returned syncronous or asycncronus
   */
  @Override
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    setMuxBus(busAddress);
    int bytesRead = controller.i2cRead(this, Integer.parseInt(this.deviceBus), deviceAddress, buffer, size);
    log.debug("i2cRead. Requested {} bytes, received {} byte", size, bytesRead);
    return bytesRead;
  }

  /**
   * TODO Add demuxing. i.e the route back to the caller The i2c will receive
   * data that neeeds to be returned syncronous or asycncronus
   */
  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
    setMuxBus(busAddress);
    controller.i2cWriteRead(this, Integer.parseInt(this.deviceBus), deviceAddress, writeBuffer, writeSize, readBuffer, readSize);
    return readBuffer.length;
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

    ServiceType meta = new ServiceType(I2cMux.class.getCanonicalName());
    meta.addDescription("Multiplexer for i2c to be able to use multiple i2c devices");
    meta.addCategory("i2c", "control");
    meta.setAvailable(true);
    meta.setSponsor("Mats");
    return meta;
  }

  @Override
  public void attachI2CControl(I2CControl control) {
    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    I2CDeviceMap devicedata = new I2CDeviceMap();
    String key = control.getName();
    if (i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getDeviceBus(), control.getDeviceAddress(), control.getName());
    } else {
      devicedata.serviceName = key;
      devicedata.busAddress = control.getDeviceBus();
      devicedata.deviceAddress = control.getDeviceAddress();
      i2cDevices.put(key, devicedata);
      control.attachI2CController(this);
    }
    broadcastState();
  }

  @Override
  public void detachI2CControl(I2CControl control) {
    // This method should delete the i2c device entry from the list of
    // I2CDevices
    // The order of the detach is important because the higher level service may
    // want to execute something that
    // needs this service to still be availabe
    log.info("detachI2CControl {}", control.getName());
    String key = control.getName();
    if (i2cDevices.containsKey(key)) {
      i2cDevices.remove(key);
      control.detachI2CController(this);
      log.info("Detached");
    } else {
      log.info("Detach failed. Not found in list of i2cDevices");
    }
    broadcastState();
  }

  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
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

  public HashMap<String, I2CDeviceMap> geti2cDevices() {
    return i2cDevices;
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
