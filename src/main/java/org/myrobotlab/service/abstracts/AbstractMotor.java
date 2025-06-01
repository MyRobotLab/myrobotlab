
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperSimple;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.GeneralMotorConfig;
import org.myrobotlab.service.data.AnalogData;
import org.myrobotlab.service.interfaces.AnalogPublisher;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

/**
 * @author GroG
 *
 *         AbstractMotor - this class contains all the data necessary for
 *         MotorController to run a motor. Functions of the MotorController are
 *         proxied through this class, with itself as a parameter
 * 
 */

abstract public class AbstractMotor<C extends GeneralMotorConfig> extends Service<C> implements MotorControl, EncoderListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AbstractMotor.class);

  /**
   * list of names of possible controllers
   */
  final protected Set<String> controllers = new HashSet<>();

  /**
   * list of possible ports
   */
  protected List<String> motorPorts = new ArrayList<>();

  /**
   * if motor is locked - no position or power commands will work
   */
  protected boolean locked = false;

  /**
   * the power level requested - varies between -1.0 &lt;--&gt; 1.0
   */
  protected Double powerInput = 0.0;

  protected Double positionInput; // aka targetPos

  protected Double positionCurrent; // aka currentPos

  /**
   * controller now is part of config,
   * isAttached is if that controller is or is not attached
   */
  protected boolean isAttached = false;

  public AbstractMotor(String n, String id) {
    super(n, id);
    // subscribeToRuntime("registered");
    // "top" half of the mapper is set by the control
    // so that we "try" to maintain a standard default of -1.0 <=> 1.0 with same
    // input limits
    // "bottom" half of the mapper will be set by the controller
    registerForInterfaceChange(MotorController.class);
    // mapper.map(min, max, -1.0, 1.0);
    // Runtime.getInstance().attachServiceLifeCycleListener(getName());
    refreshControllers();
  }

  public void onRegistered(Registration s) {
    if (s.hasInterface(MotorController.class)) {
      controllers.add(s.getName());
      broadcastState();
    }
  }

  public void onReleased(String s) {
    if (controllers.contains(s)) {
      controllers.remove(s);
      broadcastState();
    }
  }

  public Set<String> refreshControllers() {
    controllers.clear();
    controllers.addAll(Runtime.getServiceNamesFromInterface(MotorController.class));
    return controllers;
  }

  public MotorController getController() {
    return (MotorController) Runtime.getService(config.controller);
  }

  // FIXME - repair input/output
  @Override
  public double getPowerLevel() {
    return powerInput;
  }

  @Override
  public boolean isAttached(MotorController controller) {
    if (controller == null) {
      return false;
    }
    return controller.getName().equals(config.controller);
  }

  @Override
  public boolean isInverted() {
    return config.mapper.maxIn < config.mapper.minOut;
  }

  @Override
  public void lock() {
    locked = true;
    broadcastState();
  }

  @Override
  public void move(double powerInput) {
    if (locked) {
      info("%s is locked - will not move");
      return;
    }

    // FIXME make mapper.isInInputRange(x)
    double min = Math.min(config.mapper.minIn, config.mapper.maxIn);
    double max = Math.max(config.mapper.minIn, config.mapper.maxIn);

    if (powerInput < min) {
      warn("requested power %.2f is under minimum %.2f", powerInput, config.mapper.minIn);
      return;
    }

    if (powerInput > max) {
      warn("requested power %.2f is over maximum %.2f", powerInput, config.mapper.maxIn);
      return;
    }

    log.info("{}.move({})", getName(), powerInput);
    this.powerInput = powerInput;
    MotorController controller = getController();
    if (controller != null) {
      invoke("publishPowerChange", powerInput);
      invoke("publishPowerOutputChange", config.mapper.calcOutput(powerInput));
      controller.motorMove(this);
    }
  }

  @Override
  public double publishPowerChange(double powerInput) {
    return powerInput;
  }

  /**
   * the published output of this motor control
   */
  public double publishPowerOutputChange(double output) {
    return output;
  }

  @Override
  public void setInverted(boolean invert) {
    log.warn("setting {} inverted = {}", getName(), invert);
    
    // FIXME - this is residue when mapper had inverted state - 
    // which it shouldn't, "inverted" is just values of the in/out params
    
    if (!invert) {
      return;
    }

    double temp = config.mapper.minIn;
    config.mapper.minIn = config.mapper.maxIn;
    config.mapper.maxIn = temp;
    broadcastState();
  }

  @Override
  public void setMinMax(double min, double max) {

    config.mapper.minIn = min;
    config.mapper.maxIn = max;
    info("updated min %.2f max %.2f", min, max);
    broadcastState();
  }

  public void map(double minX, double maxX, double minY, double maxY) {

    config.mapper.map(minX, maxX, minY, maxY);
    broadcastState();
  }

  @Override
  public void stop() {
    // log.info("{}.stop()", getName());
    MotorController controller = getController();
    powerInput = 0.0;
    if (controller != null) {
      controller.motorStop(this);
    }
    broadcastState();
  }

  // FIXME - proxy to MotorControllerx
  @Override
  public void stopAndLock() {
    info("stopAndLock");
    move(0.0);
    lock();
    broadcastState();
  }

  @Override
  public void unlock() {
    info("unLock");
    locked = false;
    broadcastState();
  }

  @Override
  public boolean isLocked() {
    return locked;
  }

  @Override
  public void stopService() {
    super.stopService();
    MotorController controller = getController();
    if (controller != null) {
      stopAndLock();
    }
  }

  // FIXME - related to update(SensorData) no ?
  public int updatePosition(int position) {
    positionCurrent = (double) position;
    return position;
  }

  public double updatePosition(double position) {
    positionCurrent = position;
    return position;
  }

  @Override
  public double getTargetPos() {
    return positionInput;
  }

  @Override
  public void onEncoderData(EncoderData data) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setEncoder(EncoderPublisher encoder) {
    // TODO Auto-generated method stub

  }

  @Override
  public void detachMotorController(MotorController controller) {

    controller.detach(this);
    controller = null;
    config.controller = null;
    broadcastState();
  }

  /**
   * routing attach
   */
  @Override
  public void attach(Attachable service) throws Exception {
    log.info("routing attach in Abstractmotor");
    if (MotorController.class.isAssignableFrom(service.getClass())) {
      attachMotorController((MotorController) service);
      return;
    } else if (AnalogPublisher.class.isAssignableFrom(service.getClass())) {
      attachAnalogPublisher((AnalogPublisher) service);
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  @Override
  public void attachAnalogPublisher(AnalogPublisher publisher) {
    publisher.attachAnalogListener(this);
  }

  @Override
  public void detachAnalogPublisher(AnalogPublisher publisher) {
    publisher.detachAnalogListener(this);
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

    log.info("attachMotorController {}", controller.getName());

    config.controller = controller.getName();
    motorPorts = controller.getPorts();
    // TODO: KW: set a reasonable mapper. for pwm motor it's probable -1 to 1 to
    // 0 to 255 ? not sure.

    /**
     * <pre>
     * Cannot directly assign - we just want the output values of the controller's mapper
     * The process is as follows:
     *    1. user creates a motor
     *    2. user creates a motor controller
     *    3. the motor controllers map value inputs minX & minY are -1.0 to 1.0 
     *       but it has no idea what the controller needs to map that range
     *    4. hopefully the motor controller's developer created a map for the motor controller which
     *       sanely maps -1.0, 1.0 to values needed by the controller .. e.g. Saber-tooth is -128, 127
     *    5  the end user attaches the motor and motor controller - we then copy in the controllers output 
     *       values to the motor control's output of its map.
     *       So, the controller gave sane defaults, but the motor control has all the necessary configuration
     * </pre>
     */

    broadcastState();
    controller.attach(this);
  }

  @Override
  public boolean isAttached() {
    return getController() != null;
  }

  @Override
  public void detach() {
    detach(config.controller);
  }

  @Override
  public void detach(String name) {
    MotorController controller = getController();

    if (controller == null || !name.equals(controller.getName())) {
      return;
    }

    String controllerName = config.controller;
    config.controller = null;
    if (controllerName != null) {
      send(controllerName, "detach", getName());
    }
    broadcastState();
  }

  @Override
  public boolean isAttached(String name) {
    MotorController controller = getController();
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<>();
    MotorController controller = getController();
    if (controller != null) {
      ret.add(controller.getName());
    }
    return ret;
  }

  // FIXME promote to interface
  public Mapper getMapper() {
    return config.mapper;
  }

  // FIXME promote to interface
  public void setMapper(MapperSimple mapper) {
    config.mapper = mapper;
  }

  // FIXME promote to interface
  @Override
  public double calcControllerOutput() {

    return config.mapper.calcOutput(getPowerLevel());
  }

  @Override
  public void setAxis(String name) {
    config.axis = name;
    broadcastState();
  }

  @Override
  public String getAxis() {
    return config.axis;
  }

  @Override
  public void onAnalog(AnalogData data) {
    move(data.value);
  }

  @Override
  public C apply(C c) {
    GeneralMotorConfig config = super.apply(c);

    // config.mapper = new MapperLinear(config.minIn, config.maxIn,
    // config.minOut, config.maxOut);
    // mapper.setInverted(config.inverted);
    // mapper.setClip(config.clip);

    // FIXME ?? future use only ServiceConfig.listeners ?
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch (Exception e) {
        error(e);
      }
    }

    if (locked) {
      lock();
    }

    return c;
  }

}