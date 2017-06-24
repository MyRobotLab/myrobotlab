package org.myrobotlab.i2c;

import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.interfaces.I2CBusControl;
import org.myrobotlab.service.interfaces.I2CBusController;

public class I2CBus implements Attachable, I2CBusControl {

  String name;
  // transient too help prevent infinite recursion in gson
  transient I2CBusController controller;

  public I2CBus(String Name) {
    this.name = Name;
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
  public void detach(Attachable service) {
    // detach / cleanup if necessary
    // @Mats what to do here ?
    // if (controller != null) { controller.detachDevice(device);} @Grog ?
  }

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services
   * could be a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(Attachable service) {
    return (controller != null && controller == service);
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
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
  }


  @Override
  public void setDeviceBus(String deviceBus) {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void setDeviceAddress(String deviceAddress) {
    // TODO Auto-generated method stub
    
  }


  @Override
  public boolean isAttached(String name) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public void attach(Attachable service) throws Exception {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void attach(String service) throws Exception {
    // TODO Auto-generated method stub
    
  }

}
