package org.myrobotlab.i2c;

import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;

public class I2CBus implements DeviceController, I2CBusControl {

  String name;
  // transient too help prevent infinite recursion in gson
  transient I2CBusController controller;

  public I2CBus(String Name) {
    this.name = Name;
  }

  @Override
  public DeviceController getController() {
    return controller;
  }

  @Override
  public String getName() {
    return name;
  }

  public void onI2cData(int[] data) {
    // This is where the data read from the i2c bus gets returned
    // pass it back to the I2cController ( Arduino ) so that it can be
    // returned to the i2cdevice
    controller.i2cReturnData(data);

  }

  @Override
  public void detach(DeviceControl device) {
    // detach / cleanup if necessary
    // @Mats what to do here ?
    // if (controller != null) { controller.detachDevice(device);} @Grog ?
  }

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services
   * could be a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null){
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public int getDeviceCount() {
    if (controller != null) {
      return controller.getDeviceCount();
    } else {
      return 0;
    }
  }

  @Override
  public Set<String> getDeviceNames() {
    if (controller != null) {
      return controller.getDeviceNames();
    }
    return new HashSet<String>();
  }

  @Override
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
  }

}
