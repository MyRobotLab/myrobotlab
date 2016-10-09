package org.myrobotlab.service;

import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * 
 * MPU6050 - MPU-6050 sensor contains a MEMS accelerometer and a MEMS gyro in a single chip. 
 * It is very accurate, as it contains 16-bits analog to digital conversion hardware for each channel. 
 * Therefore it captures the x, y, and z channel at the same time.
 * http://playground.arduino.cc/Main/MPU-6050
 *
 * This is a port of the https://github.com/jrowberg/i2cdevlib/blob/master/Arduino/MPU6050/MPU6050.cpp
 * from Arduino C/C++ to Java. 
 * 
 */
/**
 * ============================================ I2Cdev device library code is
 * placed under the MIT license Copyright (c) 2012 Jeff Rowberg Permission is
 * hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE. ===============================================
 */

public class Bno055 extends Service implements I2CControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Bno055.class);

  StringBuilder debugRX = new StringBuilder();

  transient I2CController controller;

  public List<String> deviceAddressList = Arrays.asList("0x28", "0x29");
  public String deviceAddress = "0x28";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8");
  public String deviceBus = "1";

  public List<String> controllers;
  public String controllerName;
  public boolean isAttached = false;

  public static final int BNO055_ID = 0xA0;

  public static final int BNO055_CHIP_ID_ADDR = 0x00;
  public static final int BNO055_PAGE_ID_ADDR = 0X07;
  public static final int BNO055_EULER_H_LSB_ADDR = 0X1A;
  public static final int BNO055_OPR_MODE_ADDR = 0x3D;
  public static final int BNO055_PWR_MODE_ADDR = 0x3E;
  public static final int BNO055_SYS_TRIGGER_ADDR = 0x3F;
  
  public static final int BNO055_POWER_MODE_NORMAL = 0x00;

  public static final int BNO055_OPERATION_MODE_CONFIG = 0x00;
  public static final int BNO055_OPERATION_MODE_NDOF  = 0x0C;
  

  private int mode;

  public class BNO055Event {
    public int version;
    public long timestamp = System.currentTimeMillis();
    public class Orientation {
      public double x;
      public double y;
      public double z;
    }
    public Orientation orientation = new Orientation();
  }
  
  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();

    try {

      /*
       * Mpu6050 mpu6050 = (Mpu6050) Runtime.start("mpu6050", "Mpu6050");
       * Runtime.start("gui", "GUIService");
       */

      /*
       * Arduino arduino = (Arduino) Runtime.start("Arduino","Arduino");
       * arduino.connect("COM4"); mpu6050.setController(arduino);
       */

      /*
       * RasPi raspi = (RasPi) Runtime.start("RasPi","RasPi");
       * mpu6050.setController(raspi); mpu6050.dmpInitialize();
       */
      int[] buffer = new int[] { (int) 0xff, (int) 0xd0 };
      int a = (byte) buffer[0] << 8 | buffer[1] & 0xff;
      log.info(String.format("0xffd0 should be -48 is = %s", a));

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Bno055(String n) {
    super(n);
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
  public void startService() {
    super.startService();
  }

  /**
   * This methods sets the i2c Controller that will be used to communicate with
   * the i2c device
   */
  // @Override
  public boolean setController(String controllerName, String deviceBus, String deviceAddress) {
    return setController((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public boolean setController(String controllerName) {
    return setController((I2CController) Runtime.getService(controllerName), this.deviceBus, this.deviceAddress);
  }

  public boolean setController(I2CController controller) {
    return setController(controller, this.deviceBus, this.deviceAddress);
  }

  /**
   * This methods sets the i2c Controller that will be used to communicate with
   * the i2c device
   */
  public boolean setController(I2CController controller, String deviceBus, String deviceAddress) {
    if (controller == null) {
      log.error("setting null as controller");
      return false;
    }
    controllerName = controller.getName();
    this.controller = controller;
    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;
    isAttached = true;

    log.info(String.format("%s setController %s", getName(), controllerName));
    createDevice();
    //initialize();
    broadcastState();
    return true;
  }

  public void unsetController() {
    controller = null;
    controllerName = null;
    this.deviceBus = null;
    this.deviceAddress = null;
    isAttached = false;
    broadcastState();
  }

  public I2CController getController() {
    return controller;
  }

  public String getControllerName() {

    String controlerName = null;

    if (controller != null) {
      controlerName = controller.getName();
    }

    return controlerName;
  }

  public void setDeviceBus(String deviceBus) {
    this.deviceBus = deviceBus;
    broadcastState();
  }

  public void setDeviceAddress(String deviceAddress) {
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  public boolean isAttached() {
    return isAttached;
  }

  /**
   * This method creates the i2c device
   */
  boolean createDevice() {
    if (controller != null) {
        controller.createI2cDevice(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress));
        log.info(String.format("Created device on bus: %s address %s", deviceBus, deviceAddress));
        return true;
    }
    else {
      log.error(String.format("Cannot create device on bus: %s address, no controller selected", deviceBus, deviceAddress));
      return false;
    }
  }

  /**
   * This method reads all the 7 raw values in one go accelX accelY accelZ
   * temperature ( In degrees Celcius ) gyroX gyroY gyroZ
   * 
   */
  /** TODO Make the way it should be
   *       Currently only used for test of data binding to the 
   *       webgui
   */
  public void refresh() {
    broadcastState();
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

    ServiceType meta = new ServiceType(Mpu6050.class.getCanonicalName());
    meta.addDescription("General BNO055 acclerometer and gyro");
    meta.addCategory("microcontroller", "sensor");
    meta.setSponsor("Christian");
    return meta;
  }


  @Override
  public void setController(DeviceController controller) {
    setController(controller);
  }
  
  public boolean begin() {
    
    byte[] buffer = new byte[1];
    /* Make sure we have the right device */
    byte[] wbuffer = new byte[] {BNO055_CHIP_ID_ADDR};
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length);
    controller.i2cRead(this, Integer.parseInt(this.deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
    if ((buffer[0] & 0xFF) != BNO055_ID) {
      log.info("BNO055 sensor not found");
      return false;
    }
    /* Switch to config mode (just in case since this is the default) */
    setMode(BNO055_OPERATION_MODE_CONFIG);
    /* Reset */
    i2cWrite((byte)BNO055_SYS_TRIGGER_ADDR, (byte)0x20);
    buffer[0] = 0;
    while ((buffer[0] & 0xFF) != BNO055_ID) {
      controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length);
     controller.i2cRead(this, Integer.parseInt(this.deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
    }
    sleep(50);
    /* Set to normal power mode */
    i2cWrite((byte)BNO055_PWR_MODE_ADDR, (byte)BNO055_POWER_MODE_NORMAL);
    sleep(10);
    i2cWrite((byte)BNO055_PAGE_ID_ADDR, (byte)0);
    i2cWrite((byte)BNO055_SYS_TRIGGER_ADDR, (byte)0x00);
    sleep(10);
    setMode(BNO055_OPERATION_MODE_NDOF);
    sleep(20);
    return true;
  }
  
  public void setMode(int mode) {
    this.mode = mode;
    i2cWrite((byte)BNO055_OPR_MODE_ADDR, (byte)mode);
    sleep(30);
  }
  
  public void i2cWrite(byte address, byte value) {
    byte[] wbuffer = new byte[] {address, value};
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length);
  }
  
  public void setExtCrystalUse(boolean value) {
    int modeback = mode;
    setMode(BNO055_OPERATION_MODE_CONFIG);
    sleep(25);
    i2cWrite((byte)BNO055_PAGE_ID_ADDR, (byte)0);
    if(value) {
      i2cWrite((byte)BNO055_SYS_TRIGGER_ADDR, (byte)0x80);
    }
    else {
      i2cWrite((byte)BNO055_SYS_TRIGGER_ADDR, (byte)0x00);
    }
    sleep(10);
    setMode(modeback);
    sleep(20);
  }

  public BNO055Event getEvent() {
    BNO055Event event = new BNO055Event();
    byte[] wbuffer = new byte[] {BNO055_EULER_H_LSB_ADDR};
    byte[] rbuffer = new byte[6];
    controller.i2cWriteRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length, rbuffer, rbuffer.length);
    event.orientation.x = (((int)(rbuffer[0] & 0xFF)) | (((int)(rbuffer[1] & 0xFF)) << 8))/16.0; 
    event.orientation.y = (((int)(rbuffer[2] & 0xFF)) | (((int)(rbuffer[3] & 0xFF)) << 8))/16.0; 
    event.orientation.z = (((int)(rbuffer[4] & 0xFF)) | (((int)(rbuffer[5] & 0xFF)) << 8))/16.0; 
    return event;
    
  }
  
}
