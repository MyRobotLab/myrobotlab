
/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.abstracts;

import java.util.List;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.joystick.Component;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.ButtonDefinition;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.MotorEncoder;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

/**
 * @author GroG
 *
 *         AbstractMotor - this class contains all the data necessary for
 *         MotorController to run a motor. Functions of the MotorController are
 *         proxied through this class, with itself as a parameter
 * 
 */

abstract public class AbstractMotor extends Service implements MotorControl, EncoderListener, PinListener {

  public final static Logger log = LoggerFactory.getLogger(AbstractMotor.class);

  private static final long serialVersionUID = 1L;

  /**
   * list of names of possible controllers
   */
  public List<String> controllers;

  transient MotorEncoder encoder = null;

  // FIXME - implements an Encoder interface
  // get a named instance - stopping and tarting should not be creating &
  // destroying
  transient Object lock = new Object();
  boolean locked = false;

  /**
   * a new "un-set" mapper for merging with default motorcontroller
   */
  transient Mapper mapper = new MapperLinear();

  Double max = null;

  Double min = null;

  // feedback
  Double positionCurrent; // aka currentPos

  Double positionInput; // aka targetPos

  /**
   * the power level requested - varies between -1.0 &lt;--&gt; 1.0
   */

  // FIXME - check to see if these are necessary PROBABLY NOT SINCE THE MAPPER
  // PARTS ARE NOW PART OF CONTROLLER

  // inputs
  Double powerInput = 0.0;

  public AbstractMotor(String n, String id) {
    super(n, id);
    subscribeToRuntime("registered");
    // "top" half of the mapper is set by the control
    // so that we "try" to maintain a standard default of -1.0 <=> 1.0 with same
    // input limits
    // "bottom" half of the mapper will be set by the controller
    mapper.map(-1.0, 1.0, -1.0, 1.0);
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (MotorController.class.isAssignableFrom(service.getClass())) {
      attachMotorController((MotorController) service);
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  public void attach(ButtonDefinition buttondef) {
    subscribe(buttondef.getName(), "publishButton", getName(), "move");
  }

  public void attach(Component joystickComponent) {
    if (joystickComponent == null) {
      error("cannot attach a null joystick component", getName());
      return;
    }
    send(joystickComponent.getName(), "addListener", getName(), joystickComponent.id);
  }

  public void attach(PinDefinition pindef) {
    // SINGLE PIN MAN !! not ALL PINS !
    // must be local now :P
    // FIXME this "should" be cable of adding vi
    // e.g send(pindef.getName(), "attach", getName(), pindef.getAddress());
    // attach(pindef.getName(), pindef.getAddress)
    PinArrayControl pac = (PinArrayControl) Runtime.getService(pindef.getName());
    pac.attach(this, pindef.getAddress());
    // subscribe(pindef.getName(), "publishPin", getName(), "move");
  }

  @Override
  public void attachMotorController(MotorController controller) throws Exception {
    if (controller == null) {
      error("motor.attach(controller) - controller cannot be null");
      return;
    }
    if (isAttached(controller)) {
      log.info("motor {} already attached to motor controller {}", getName(), controller.getName());
      return;
    }

    // FIXME - setup subscriptions
    addListener("publishMotorMove", controller.getName());
    addListener("publishMotorStop", controller.getName());

    // TODO: KW: set a reasonable mapper. for pwm motor it's probable -1 to 1 to
    // 0 to 255 ? not sure.
    this.mapper = controller.getDefaultMapper();

    broadcastState();
    controller.attach(this);
  }

  // FIXME promot to interface
  public double calcControllerOutput() {
    return mapper.calcOutput(getPowerLevel());
  }

  // FIXME promote to interface
  public Mapper getMapper() {
    return mapper;
  }

  // FIXME - repair input/output
  @Override
  public double getPowerLevel() {
    return powerInput;
  }

  @Override
  public double getTargetPos() {
    return positionInput;
  }

  @Override
  public boolean isInverted() {
    return mapper.isInverted();
  }

  @Override
  public boolean isLocked() {
    return locked;
  }

  @Override
  public void lock() {
    info("%s.lock", getName());
    locked = true;
    broadcastState();
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    mapper.map(minX, maxX, minY, maxY);
    broadcastState();
  }

  @Override
  public void move(double powerInput) {
    info("%s.move(%.2f)", getName(), powerInput);
    this.powerInput = powerInput;
    broadcast("publishMotorMove", this);
    broadcastState();
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // TODO Auto-generated method stub

  }

  // hmm
  public void onJoystickData(JoystickData data) {
    // info("AbstractMotor onJoystickData - %f", data.value);
    Double pwr = null;
    pwr = data.value.doubleValue();
    move(pwr);
  }

  // hmm
  public void onPin(PinData data) {
    Double pwr = null;

    pwr = data.value.doubleValue();

    move(pwr);
  }

  public void onRegistered(Registration s) {
    refreshControllers();
    broadcastState();
  }

  public MotorControl publishMotorMove(MotorControl motor) {
    return motor;
  }

  public MotorControl publishMotorStop(MotorControl motor) {
    return motor;
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(MotorController.class);
    return controllers;
  }

  @Override
  public void setEncoder(EncoderPublisher encoder) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setInverted(boolean invert) {
    mapper.setInverted(invert);
    broadcastState();
  }

  // FIXME promote to interface
  public void setMapper(Mapper mapper) {
    this.mapper = mapper;
  }

  //////////////// begin new stuff ///////////////////////

  // ---- Servo begin ---------
  public void setMinMax(double min, double max) {
    this.min = min;
    this.max = max;
    broadcastState();
  }

  @Override
  public void stop() {
    // log.info("{}.stop()", getName());
    powerInput = 0.0;
    broadcast("publishMotorStop", this);
    broadcastState();
  }

  // FIXME - proxy to MotorControllerx
  @Override
  public void stopAndLock() {
    info("stopAndLock");
    stop();
    lock();
    broadcastState();
  }

  @Override
  public void stopService() {
    super.stopService();
    stopAndLock();
  }

  @Override
  public void unlock() {
    info("unLock");
    locked = false;
    broadcastState();
  }

  // FIXME - EncoderData !!!
  public double updatePosition(double position) {
    positionCurrent = position;
    return position;
  }

  // FIXME - related to update(SensorData) no ?
  public int updatePosition(int position) {
    positionCurrent = (double) position;
    return position;
  }
}