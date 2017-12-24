/*
 * 
 *   AdafruitMotorHat4Pi
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.RasPi;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.slf4j.Logger;

/**
 * AdaFruit DC And Stepper Motor HAT for Raspberry PI
 * 
 * @author Mats
 * 
 *         References :
 *         https://learn.adafruit.com/adafruit-dc-and-stepper-motor-hat-for-raspberry-pi/overview
 */

public class AdafruitMotorHat4Pi extends AbstractMotorController implements I2CControl {

  /** version of the library */
  static public final String VERSION = "0.9";

  private static final long serialVersionUID = 1L;

  transient public I2CController controller;

  int pwmFreq;
  boolean pwmFreqSet = false;

  // List of possible addresses. Used by the GUI. Address 0x70 is excluded,
  // because it is a broadcast address that all boards will respond to
  public List<String> deviceAddressList = Arrays.asList("0x60", "0x61", "0x62", "0x63", "0x64", "0x65", "0x66", "0x67", "0x68", "0x69", "0x6A", "0x6B", "0x6C", "0x6D", "0x6E",
      "0x6F", "0x71", "0x72", "0x73", "0x74", "0x75", "0x76", "0x77", "0x78", "0x79", "0x7A", "0x7B", "0x7C", "0x7D", "0x7E", "0x7F");
  // Default address
  public String deviceAddress = "0x60";
  /**
   * This address is to address all PCA9685 on the i2c bus. Don't use this
   * address for any other device on the i2c bus since it will cause collisions.
   */
  public String broadcastAddress = "0x70";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public transient final static Logger log = LoggerFactory.getLogger(AdafruitMotorHat4Pi.class.getCanonicalName());

  public static final int PCA9685_MODE1 = 0x00; // Mod
  // 1
  // register
  public static final byte PCA9685_SLEEP = 0x10; // Set sleep mode before
                                                 // changing prescale value
  public static final byte PCA9685_AUTOINCREMENT = 0x20; // Set autoincrement to
                                                         // be able to write
                                                         // more than one byte
                                                         // in sequence

  public static final byte PCA9685_PRESCALE = (byte) 0xFE; // PreScale register

  // Pin PWM addresses 4 bytes repeats for each pin so I only define pin 0
  // The rest of the addresses are calculated based on pin numbers
  public static final int PCA9685_LED0_ON_L = 0x06; // First LED address Low
  public static final int PCA9685_LED0_ON_H = 0x07; // First LED address High
  public static final int PCA9685_LED0_OFF_L = 0x08; // First LED address Low
  public static final int PCA9685_LED0_OFF_H = 0x08; // First LED addressHigh

  public static final int PCA9685_ALL_LED_OFF_H = 0xFD; // All call i2c address
                                                        // ( Used for shutdown
                                                        // of all pwm )
  public static final int PCA9685_TURN_ALL_LED_OFF = 0x10; // Command to turn
                                                           // all LED off stop
                                                           // pwm )

  public static final float osc_clock = 25000000; // clock frequency of the
                                                  // internal clock
  public static final float precision = 4096; // pwm_precision

  // i2c controller
  public List<String> controllers;
  public String controllerName;

  // isAttached is used by the GUI's to know it the service is attached or not
  // It will be set when the first successful communication has been done with
  // the
  // i2c device ( bus and address have been verified )
  public boolean isAttached = false;

  class MotorData {
    String motorId;
    int pwmPin;
    int in1;
    int in2;
  }

  transient HashMap<String, MotorData> motorMap = new HashMap<String, MotorData>();

  // Motor related constants
  public static final int MOTOR_FORWARD = 1;
  public static final int MOTOR_BACKWARD = 0;
  public static final int defaultMotorPwmFreq = 1600;

  public static void main(String[] args) {

    try {
      LoggingFactory.getInstance().configure();
      LoggingFactory.getInstance().setLevel(Level.DEBUG);

      SwingGui swing = (SwingGui) Runtime.start("gui", "SwingGui");
      RasPi raspi = (RasPi) Runtime.start("raspi", "RasPi");
      AdafruitMotorHat4Pi hat = (AdafruitMotorHat4Pi) Runtime.start("hat", "AdafruitMotorHat4Pi");
      hat.attach(raspi, "1", "0x60");
      MotorHat4Pi m1 = (MotorHat4Pi) Runtime.start("m1", "MotorHat4Pi");
      MotorHat4Pi m2 = (MotorHat4Pi) Runtime.start("m2", "MotorHat4Pi");
      MotorHat4Pi m3 = (MotorHat4Pi) Runtime.start("m3", "MotorHat4Pi");
      MotorHat4Pi m4 = (MotorHat4Pi) Runtime.start("m4", "MotorHat4Pi");
      m1.setMotor("M1");
      m2.setMotor("M2");
      m3.setMotor("M3");
      m4.setMotor("M4");
      m1.attach(hat);
      m2.attach(hat);
      m3.attach(hat);
      m4.attach(hat);
      m3.move(0.6);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AdafruitMotorHat4Pi(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
    powerMapper = new Mapper(-1.0, 1.0, -1.0, 1.0);
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  /*
   * Refresh the list of running services that can be selected in the GUI In
   * this case only RasPi service can be selected
   */
  public List<String> refreshControllers() {

    controllers = new ArrayList<String>();
    for (String serviceName : Runtime.getServiceNamesFromInterface(I2CController.class)) {
      if (Runtime.getService(serviceName).getClass() == RasPi.class) {
        controllers.add(serviceName);
      }
    }
    return controllers;
  }

  /*
   * Set the PWM pulsewidth
   * 
   */
  public void setPWM(Integer pin, Integer pulseWidthOn, Integer pulseWidthOff) {

    byte[] buffer = { (byte) (PCA9685_LED0_ON_L + (pin * 4)), (byte) (pulseWidthOn & 0xff), (byte) (pulseWidthOn >> 8), (byte) (pulseWidthOff & 0xff),
        (byte) (pulseWidthOff >> 8) };
    // log.info(String.format("Writing pin %s, pulesWidthOn %s, pulseWidthOff %s", pin, pulseWidthOn, pulseWidthOff));
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  /*
   * Set the PWM frequency i.e. the frequency between positive pulses.
   * 
   */
  public void setPWMFreq(int pin, Integer hz) { // Analog servos run at ~60 Hz
    final int minPwmFreq = 24;
    final int maxPwmFreq = 1526;
    float prescale_value;

    if (hz < minPwmFreq) {
      log.error(String.format("Minimum PWMFreq is %s Hz, requested freqency is %s Hz, clamping to minimum", minPwmFreq, hz));
      hz = minPwmFreq;
      prescale_value = 255;
    } else if (hz > maxPwmFreq) {
      log.error(String.format("Maximum PWMFreq is %s Hz, requested frequency is %s Hz, clamping to maximum", maxPwmFreq, hz));
      hz = maxPwmFreq;
      prescale_value = 3;
    } else {
      // Multiplying with factor 0.9 to correct the frequency
      // See
      // https://github.com/adafruit/Adafruit-PWM-Servo-Driver-Library/issues/11
      prescale_value = Math.round(0.9 * osc_clock / precision / hz) - 1;
    }

    // log.info(String.format("PWMFreq %s hz, prescale_value calculated to %s", hz, prescale_value));
    // Set sleep mode before changing PWM freqency
    byte[] writeBuffer = { PCA9685_MODE1, PCA9685_SLEEP };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writeBuffer, writeBuffer.length);

    // Wait 1 millisecond until the oscillator has stabilized
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      if (Thread.interrupted()) { // Clears interrupted status!
      }
    }

    // Write the PWM frequency value
    byte[] buffer2 = { PCA9685_PRESCALE, (byte) prescale_value };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer2, buffer2.length);

    // Leave sleep mode, set autoincrement to be able to write several
    // bytes
    // in sequence
    byte[] buffer3 = { PCA9685_MODE1, PCA9685_AUTOINCREMENT };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer3, buffer3.length);

    // Wait 1 millisecond until the oscillator has stabilized
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      if (Thread.interrupted()) { // Clears interrupted status!
      }
    }

    pwmFreq = hz;
    pwmFreqSet = true;

  }

  /*
   * Orderly shutdown. Send a message to stop all pwm generation
   * 
   */
  public void stopPwm() {

    byte[] buffer = { (byte) (PCA9685_ALL_LED_OFF_H), (byte) PCA9685_TURN_ALL_LED_OFF };
    // log.info(String.format("Writing shutdown command to %s", this.getName()));
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  public String publishAttachedDevice(String deviceName) {
    return deviceName;
  }

  @Override
  public void motorMove(MotorControl mc) {

    Class<?> type = mc.getClass();

    double powerOutput = powerMapper.calcOutput(mc.getPowerLevel ());
    // log.info(String.format("powerOutput = %.3f", powerOutput));

    // Clamp powerOutput between -1 and 1

    if (powerOutput > 1)
      powerOutput = 1;
    if (powerOutput < -1)
      powerOutput = -1;

    if (MotorHat4Pi.class == type) {
      MotorHat4Pi motor = (MotorHat4Pi) mc;
      if (motor.getPwmFreq() == null) {
        motor.setPwmFreq(defaultMotorPwmFreq);
        setPWMFreq(motor.getPwmPin(), motor.getPwmFreq());
      }
      // log.info(String.format("AdafruitMotorHat4Pi, powerOutput = %s", powerOutput));
      if (powerOutput < 0) {
        setPinValue(motor.getLeftDirPin(), 0);
        setPinValue(motor.getRightDirPin(), 1);
      } else if (powerOutput > 0) {
        setPinValue(motor.getLeftDirPin(), 1);
        setPinValue(motor.getRightDirPin(), 0);
      } else {
        setPinValue(motor.getLeftDirPin(), 0);
        setPinValue(motor.getRightDirPin(), 0);
      }
      setPinValue(motor.getPwmPin(), Math.abs(powerOutput));
    } else {
      error("motorMove for motor type %s not supported", type);
    }
  }

  public void setPinValue(int pin, double powerOutput) {
    // log.info(String.format("setPinValue, pin = %s, powerOutput = %s", pin, powerOutput));
    if (powerOutput < 0) {
      log.error(String.format("setPinValue. Value below zero (%s). Defaulting to 0.", powerOutput));
      powerOutput = 0;
    } else if (powerOutput > 1) {
      log.error(String.format("setPinValue. Value > 1 (%s). Defaulting to 1", powerOutput));
      powerOutput = 1;
    }

    int powerOn;
    int powerOff;
    // No phase shift. Simple calculation
    if (powerOutput == 0) {
      powerOn = 4096;
      powerOff = 0;
    } else if (powerOutput == 1) {
      powerOn = 0;
      powerOff = 4096;
    } else {
      powerOn = 0;
      powerOff = (int) (powerOutput * 4096);
    }
    // log.info(String.format("powerOutput = %s, powerOn = %s, powerOff = %s", powerOutput, powerOn, powerOff));
    setPWM(pin, powerOn, powerOff);
  }

  @Override
  public void motorMoveTo(MotorControl mc) {
    /*
     * TODO Implement MotorMove for the Stepper motor(s)
     */
  }

  @Override
  public void motorStop(MotorControl mc) {

    Class<?> type = mc.getClass();

    if (MotorHat4Pi.class == type) {
      MotorHat4Pi motor = (MotorHat4Pi) mc;
      if (motor.getPwmFreq() == null) {
        motor.setPwmFreq(defaultMotorPwmFreq);
        setPWMFreq(motor.getPwmPin(), motor.getPwmFreq());
      }
      setPinValue(motor.getLeftDirPin(), 0);
      setPinValue(motor.getRightDirPin(), 0);
      setPinValue(motor.getPwmPin(), 0);
    } else {
      error("motorStop for motor type %s not supported", type);
    }

  }

  @Override
  public void motorReset(MotorControl motor) {
    // perhaps this should be in the motor control
    // motor.reset();
    // opportunity to reset variables on the controller
    // sendMsg(MOTOR_RESET, motor.getind);

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

    ServiceType meta = new ServiceType(AdafruitMotorHat4Pi.class.getCanonicalName());
    meta.addDescription("Adafruit DC and Stepper Motor Hat for Raspberry PI");
    meta.addCategory("shield", "servo", "pwm");
    meta.setSponsor("Mats");
    meta.addPeer("raspi", "RasPi", "Raspberry PI");
    meta.setAvailable(true);
    return meta;
  }

  /*
   * @Override public boolean isAttached(String name) { return (controller !=
   * null && controller.getName().equals(name) || servoMap.containsKey(name)); }
   */

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    ;
    return false;
  }

  @Override
  public void stopService() {
    if (!isAttached(controller)) {
      detachI2CController(controller);
    }
    super.stopService(); // stop inbox and outbox
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
    /*
     * TODO Implement attachMotorControl if
     * (MotorControl.class.isAssignableFrom(service.getClass())) {
     * attachMotorControl((MotorControl) service); return; }
     */
  }

  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
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

    /*
     * TODO Implement detachMotorControl
     * 
     * if (MotorControl.class.isAssignableFrom(service.getClass())) { try {
     * detachMotorControl((MotorControl) service); } catch (Exception e) { //
     * TODO Auto-generated catch block);
     * log.error("setController / attach throw", e); } return; }
     */
  }

  @Override
  public void detachI2CController(I2CController controller) {

    if (!isAttached(controller))
      return;

    stopPwm(); // stop pwm generation
    isAttached = false;
    controller.detachI2CControl(this);
    broadcastState();
  }

  /**
   * Returns all the currently attached services TODO Add the list of attached
   * motors
   */
  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null && isAttached) {
      ret.add(controller.getName());
    }
    ret.addAll(motorMap.keySet());
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

  /**
   * This type of motordriver doesn't use any ports
   */
  @Override
  public List<String> getPorts() {
    // This type of motordriver does not use any ports
    return null;
  }

}