package org.myrobotlab.i2c;

import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.service.interfaces.Attachable;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;
import org.myrobotlab.service.interfaces.ServiceInterface;

public class I2CBus implements Attachable, I2CBusControl {

  String name;
  // transient too help prevent infinite recursion in gson
  transient I2CBusController controller;

  public I2CBus(String Name) {
    this.name = Name;
  }

  // @Override
  public I2CBusController getController() {
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

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services could be
   * a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public int getAttachedCount() {
    if (controller != null) {
      return controller.getAttachedCount();
    } else {
      return 0;
    }
  }

  @Override
  public Set<String> getAttachedNames() {
    if (controller != null) {
      return controller.getAttachedNames();
    }
    return new HashSet<String>();
  }

  @Override
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(getName());
    controller = null;
  }

}
