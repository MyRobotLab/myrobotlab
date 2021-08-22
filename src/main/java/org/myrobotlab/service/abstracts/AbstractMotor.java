
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
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.AbstractMotorConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.AnalogData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.AnalogListener;
import org.myrobotlab.service.interfaces.AnalogPublisher;
import org.myrobotlab.service.interfaces.ButtonDefinition;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
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

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AbstractMotor.class);

  protected transient MotorController controller = null;

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
   * attached analog publishers to this service - functionally its
   * a simple "lock" to avoid cyclic attach/detaches - works well
   */
  // final protected Set<String> analogPublishers = new HashSet<>();
  // bad idea publishers internally will need to know about subscribers
  // but not the other way around ... could this be a general pattern for
  // how to manage attach? ie publishers attach "always" needs to be called
  // subscribers can just call publishers attach with their attach

  /**
   * the power level requested - varies between -1.0 &lt;--&gt; 1.0
   */
  protected Double powerInput = 0.0;

  protected Double positionInput; // aka targetPos

  protected Double positionCurrent; // aka currentPos

  /**
   * a new "un-set" mapper for merging with default motorcontroller
   */
  protected MapperLinear mapper = new MapperLinear();

  /**
   * name of the currently attached motor controller
   */
  protected String controllerName;

  /**
   * min - defaults to -1.0 equivalent of 100% power rotation ccw
   */
  protected double min = -1.0;

  /**
   * max - defaults to 1.0 equivalent of 100% power cw rotation
   */
  protected double max = 1.0;

  private String axisName;

  public AbstractMotor(String n, String id) {
    super(n, id);
    // subscribeToRuntime("registered");
    // "top" half of the mapper is set by the control
    // so that we "try" to maintain a standard default of -1.0 <=> 1.0 with same
    // input limits
    // "bottom" half of the mapper will be set by the controller
    mapper.map(min, max, -1.0, 1.0);
    registerForInterfaceChange(MotorController.class);
    // Runtime.getInstance().subscribeToLifeCycleEvents(getName());
    // refreshControllers();
  }
  
  @Override
  public void onAddInterface(String serviceName, String interfaceName) {
    if (MotorController.class.toString().equals(interfaceName)) {
      controllers.add(serviceName);
      broadcastState();
    }
  }

  @Override
  public void onRemoveInterface(String serviceName, String interfaceName) {
    if (MotorController.class.toString().equals(interfaceName)) {
      controllers.remove(serviceName);
      broadcastState();
    }
  }


  /*
  @Override
  public void onRegistered(Registration s) {
    if (s.hasInterface(MotorController.class)) {
      controllers.add(s.getName());
      broadcastState();  
    }    
  }

  @Override
  public void onReleased(String s) {
    if (controllers.contains(s)) {
      controllers.remove(s);
      broadcastState();
    }
  }
  */
  
  public Set <String> refreshControllers() {
    controllers.clear();
    controllers.addAll(Runtime.getServiceNamesFromInterface(MotorController.class));
    broadcastState();
    return controllers;
  }

  public MotorController getController() {
    return controller;
  }

  // FIXME - repair input/output
  @Override
  public double getPowerLevel() {
    return powerInput;
  }

  @Override
  public boolean isAttached(MotorController controller) {
    return this.controller == controller;
  }

  @Override
  public boolean isInverted() {
    return mapper.isInverted();
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

    if (powerInput < min) {
      warn("requested power %.2f is under minimum %.2f", powerInput, min);
      return;
    }

    if (powerInput > max) {
      warn("requested power %.2f is over maximum %.2f", powerInput, max);
      return;
    }

    log.info("{}.move({})", getName(), powerInput);
    this.powerInput = powerInput;
    
    invoke("publishPowerChange", powerInput);
    invoke("publishPowerOutputChange", mapper.calcOutput(powerInput));
    
    if (controller != null) {
      controller.motorMove(this);
    }

  }

  @Override
  public double publishPowerChange(double powerInput) {
    return powerInput;
  }
  
  public double publishPowerOutputChange(double powerOutput) {
    return powerOutput;
  }

  @Override
  public void setInverted(boolean invert) {
    log.warn("setting {} inverted = {}", getName(), invert);
    mapper.setInverted(invert);
    broadcastState();
  }

  @Override
  public void setMinMax(double min, double max) {
    this.min = min;
    this.max = max;
    info("updated min %.2f max %.2f", min, max);
    broadcastState();
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    mapper.map(minX, maxX, minY, maxY);
    broadcastState();
  }

  @Override
  public void stop() {
    // log.info("{}.stop()", getName());
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

  public void detachMotorController(MotorController controller) {
    controller.detach(this);
    controller = null;
    controllerName = null;
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


  @Deprecated /* I think this was an attempt to control via an analog pin - should be updated to use attachAnalogPublisher */
  public void onPin(PinData data) {
    Double pwr = null;
    pwr = data.value.doubleValue();
    move(pwr);
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

  public void attach(ButtonDefinition buttondef) {
    subscribe(buttondef.getName(), "publishButton", getName(), "move");
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

    this.controller = controller;
    controllerName = controller.getName();
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
    Mapper defaultControllerMapper = controller.getDefaultMapper();
    // interesting idea - but this conflicts with user set config - removing
    // mapper.map(mapper.getMinX(), mapper.getMaxX(), defaultControllerMapper.getMinY(), defaultControllerMapper.getMaxY());

    broadcastState();
    controller.attach(this);
  }

  @Override
  public boolean isAttached() {
    return controller != null;
  }

  public void detach() {
    super.detach();
    if (controller != null) {
      detach(controller.getName());
    }
  }

  // FIXME - clean up the attach/detach
  // TODO - this could be Java 8 default interface implementation
  @Override
  public void detach(String name) {
    if (controller == null || !name.equals(controller.getName())) {
      return;
    }
    controller.detach(this); // FIXME - call
                             // detachMotorController(MotorController
    controllerName = null;
    controller = null;
    broadcastState();
  }

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

  // FIXME promote to interface
  public Mapper getMapper() {
    return mapper;
  }

  // FIXME promote to interface
  public void setMapper(MapperLinear mapper) {
    this.mapper = mapper;
  }

  // FIXME promote to interface
  public double calcControllerOutput() {
    return mapper.calcOutput(getPowerLevel());
  }

  @Override
  public void setAnalogId(String name) {
    axisName = name;
  }

  @Override
  public String getAnalogId() {
    return axisName;
  }

  @Override
  public void onAnalog(AnalogData data) {
    move(data.value);
  }

  @Override /* incoming config is from derived motor type */
  protected ServiceConfig initConfig(ServiceConfig c) {
    super.initConfig(c);
    AbstractMotorConfig config = (AbstractMotorConfig)c;

    config.locked = locked;

    if (mapper != null) {
      config.clip = mapper.isClip();
      config.maxIn = mapper.getMaxX();
      config.maxOut = mapper.getMaxY();
      config.minIn = mapper.getMinX();
      config.minOut = mapper.getMinY();
      config.inverted = mapper.isInverted();
    }

    return config;
  }

  public ServiceConfig load(ServiceConfig c) {
    AbstractMotorConfig config = (AbstractMotorConfig) c;

    mapper = new MapperLinear(config.minIn, config.maxIn, config.minOut, config.maxOut);
    mapper.setInverted(config.inverted);
    mapper.setClip(config.clip);

    if (locked) {
      lock();
    }

    return c;
  }

}