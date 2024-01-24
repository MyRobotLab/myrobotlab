package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.I2cMuxConfig;
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
 * @author Mats Onnerby, GroG
 * 
 *         More Info : https://www.adafruit.com/product/2717
 * 
 */
public class I2cMux extends Service<I2cMuxConfig> implements I2CControl, I2CController {

  public static class I2CDeviceMap {
    public String busAddress;
    public String deviceAddress;
    public String serviceName;
  }

  protected final static Logger log = LoggerFactory.getLogger(I2cMux.class.getCanonicalName());

  private static final long serialVersionUID = 1L;

  public static void main(String[] args) {
    LoggingFactory.init("info");

    try {
      I2cMux i2cMux = (I2cMux) Runtime.start("i2cMux", "I2CMux");
      i2cMux.setDeviceBus("0");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  protected transient I2CController controller;

  protected List<String> controllers = new ArrayList<String>();
  
  protected List<String> deviceAddressList = Arrays.asList("0x70", "0x71", "0x72", "0x73", "0x74", "0x75", "0x76", "0x77");
  
  protected List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");

  protected boolean isAttached = false;

  protected int lastBusAddress = -1;

  public I2cMux(String n, String id) {
    super(n, id);    
  }

  @Override
  public void attach(Attachable service) throws Exception {

    if (service == null) {
      warn("cannot attach to null");
      return;
    }
    
    if (I2CController.class.isAssignableFrom(service.getClass())) {
      attachI2CController((I2CController) service);
    } else {
      warn("do not know how to attach to %s", service.getName());
    }
  }

  @Override
  @Deprecated /*use attach(String) */
  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error("Already attached to {}, use detach({}) first", config.controller);
    }

    config.controller = controller.getName();
    log.info("{} attach {}", getName(), config.controller);

    config.bus = deviceBus;
    config.address = deviceAddress;

    attachI2CController(controller);
  }

  // This section contains all the new attach logic
  @Override
  public void attach(String service) throws Exception {
    attach(Runtime.getService(service));
  }

  @Deprecated /* use attach(String) */
  public void attach(String controller, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controller), deviceBus, deviceAddress);
  }

  @Override
  public void attachI2CControl(I2CControl control) {
    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    I2CDeviceMap devicedata = new I2CDeviceMap();
    String key = control.getName();
    if (config.i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getBus(), control.getAddress(), control.getName());
    } else {
      devicedata.serviceName = key;
      devicedata.busAddress = control.getBus();
      devicedata.deviceAddress = control.getAddress();
      config.i2cDevices.put(key, devicedata);
      control.attachI2CController(this);
    }
    broadcastState();
  }

  @Override
  public void attachI2CController(I2CController controller) {
    if (controller == null) {
      error("controller can not be null");
      return;
    }

    if (isAttached(controller)) {
      log.info("controller {} is attached", controller.getName());
      return;
    }

    this.controller = controller;
    isAttached = true;
    config.controller = controller.getName();
    // FIXME should use attach(string)
    controller.attachI2CControl(this);
    broadcastState();
    log.info("Attached {} device on bus: {} address {}", config.controller, config.bus, config.address);
  }
  
  public void detach() {
    detach(controller);
  }

  @Override
  public void detach(Attachable service) {
    if (service == null) {
      warn("detach null");
      return;
    }

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
    } else {
      warn("do not know how to attach to %s", service.getName());
    }
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach(Runtime.getService(service));
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
    if (config.i2cDevices.containsKey(key)) {
      config.i2cDevices.remove(key);
      control.detachI2CController(this);
      log.info("Detached");
    } else {
      log.info("Detach failed. Not found in list of i2cDevices");
    }
    broadcastState();
  }

  @Override
  public void detachI2CController(I2CController controller) {
    isAttached = false;
    controller.detachI2CControl(this);
    broadcastState();
  }

  @Override
  public String getAddress() {
    return config.address;
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
    return config.bus;
  }

  @Override
  public String getDeviceAddress() {
    return config.address;
  }

  @Override
  public String getDeviceBus() {
    return config.bus;
  }

  public HashMap<String, I2CDeviceMap> geti2cDevices() {
    return config.i2cDevices;
  }

  /**
   * TODO Add demuxing. i.e the route back to the caller The i2c will receive
   * data that neeeds to be returned syncronous or asycncronus
   */
  @Override
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    setMuxBus(busAddress);
    // FIXME - sendBlocking or pub/sub to/from controller
    int bytesRead = controller.i2cRead(this, Integer.parseInt(config.bus), deviceAddress, buffer, size);
    log.debug("i2cRead. Requested {} bytes, received {} byte", size, bytesRead);
    return bytesRead;
  }
    
  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    setMuxBus(busAddress);
    String key = String.format("%d.%d", busAddress, deviceAddress);
    log.debug(String.format("i2cWrite busAddress x%02X deviceAddress x%02X key %s", busAddress, deviceAddress, key));
    // FIXME - would be trivial to fix with a send(controller, Integer.parseInt(config.bus), deviceAddress, buffer, size)
    // but the read would either need to be pubsub or sendblocking
    controller.i2cWrite(this, Integer.parseInt(config.bus), deviceAddress, buffer, size);
  }

  /**
   * TODO Add demuxing. i.e the route back to the caller The i2c will receive
   * data that neeeds to be returned syncronous or asycncronus
   */
  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {
    setMuxBus(busAddress);
    controller.i2cWriteRead(this, Integer.parseInt(config.bus), deviceAddress, writeBuffer, writeSize, readBuffer, readSize);
    return readBuffer.length;
  }

  /**
   * Returns the current state of the service, if attached returns true, false
   * if it's not attached.
   * 
   * @return
   */
  public boolean isAttached() {
    return isAttached;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && instance != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    // only one controller possible
    return isAttached;
  }

  @Override
  public void setAddress(String address) {
    if (isAttached) {
      warn("already attached to %s, use detach first", config.controller);
      return;
    }
    config.address = address;
  }

  @Override
  public void setBus(String bus) {
    if (isAttached) {
      warn("already attached to %s, use detach first", config.controller);
      return;
    }
    config.bus = bus;
  }

  public void setController(String controller) {    
    config.controller = controller;
  }

  /**
   * Sets the I2C Address of the i2cMux device.
   * 
   * @param address
   *          default "0x70" range "0x70" - "0x77"
   */
  @Override
  public void setDeviceAddress(String address) {
    setAddress(address);
  }

  /**
   * Sets the I2C Bus the i2cMux is attached to.
   * 
   * @param deviceBus
   *          default is "1", range "0" - "7".
   * 
   */
  @Override
  public void setDeviceBus(String deviceBus) {
    setBus(deviceBus);
  }
  
  @Override
  public I2cMuxConfig apply(I2cMuxConfig c) {
    super.apply(c);
    if (c.controller != null) {
      try {
        attach(c.controller);
      } catch (Exception e) {
        error(e);
      }
    }
    return c;
  }

  /**
   * Sets which bus future commands will be sent down.
   * 
   * @param busAddress
   *          Range 0 - 7
   */
  public void setMuxBus(int busAddress) {
    if (busAddress != lastBusAddress) {
      byte bus[] = new byte[1];
      bus[0] = (byte) (1 << busAddress);
      log.debug("setMux config.bus {} config.address {} bus[0] {}", config.bus, config.address, bus[0]);
      controller.i2cWrite(this, Integer.parseInt(config.bus), Integer.decode(config.address), bus, bus.length);
      lastBusAddress = busAddress;
    }
  }



}
