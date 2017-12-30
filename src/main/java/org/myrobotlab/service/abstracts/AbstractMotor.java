
/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.abstracts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.sensor.Encoder;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.service.Joystick.Component;
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
 *         represents a common continuous direct current electric motor. The
 *         motor will need a MotorController which can be one of many different
 *         types of electronics. A simple H-bridge typically has 2 bits which
 *         control the motor. A direction bit which changes the polarity of the
 *         H-bridges output
 * 
 */

// extends Service implements MotorControl, EncoderListener
abstract public class AbstractMotor extends Service implements MotorControl, EncoderListener, PinListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AbstractMotor.class);

  protected transient MotorController controller = null;
  /**
   * list of names of possible controllers
   */
  public List<String> controllers;

  boolean locked = false;
  
  Mapper inputMapper;

  /**
   * the power level requested - varies between -1.0 &lt;--&gt; 1.0
   */

  double powerLevel = 0;
  double maxPower = 1.0;
  double minPower = -1.0;

  // grog: THIS SHOULD ONLY BE USED FOR INVERTED AND INPUT LIMITS !!!!
  // SHOULD NOT BE USED FOR RANGE MAPPING - RANGE MAPPING SHOULD ONLY BE
  // IN MOTORCONTROLLER !!!
  Mapper powerMap = new Mapper(-1.0, 1.0, -1.0, 1.0);

  // position
  double currentPos = 0;
  double targetPos = 0;

  Mapper encoderMap = new Mapper(-800.0, 800.0, -800.0, 800.0);

  transient MotorEncoder encoder = null;

  // FIXME - implements an Encoder interface
  // get a named instance - stopping and tarting should not be creating &
  // destroying
  transient Object lock = new Object();

  String controllerName;

  public AbstractMotor(String n) {
    super(n);
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(MotorController.class);
    return controllers;
  }

  public MotorController getController() {
    return controller;
  }

  @Override
  public double getPowerLevel() {
    return powerLevel;
  }
  
  
  /*

  // FIXME - remove !!! - no need or desire to map inside controller !
  @Override
  public double getPowerOutput() {
    return powerMap.calcOutput(powerLevel);
  }

  
  // FIXME - remove !!! - no need or desire to map inside controller !
  public Mapper getPowerMap() {
    return powerMap;
  }
  
  // FIXME - remove !!! - no need or desire to map inside controller !
  public void mapPower(double minX, double maxX, double minY, double maxY) {
    powerMap = new Mapper(minX, maxX, minY, maxY);
    broadcastState();
  }
  
  */
  

  @Override
  public boolean isAttached(MotorController controller) {
    return this.controller == controller;
  }

  @Override
  public boolean isInverted() {
    return powerMap.isInverted();
  }

  @Override
  public void lock() {
    // log.info("lock");
    locked = true;
  }

  public void mapEncoder(double minX, double maxX, double minY, double maxY) {
    encoderMap = new Mapper(minX, maxX, minY, maxY);
    broadcastState();
  }

  
  @Override
  // not relative ! - see moveStep
  public void move(double powerInput) {
    double power = powerMap.calcOutput(powerInput);
    // info("%s.move(%.3f)", getName(), power);
        
    if (Math.abs(power) > maxPower) {
      warn("motor %s.move(%.3f) out of range - must be between -1.0 and 1.0", getName(), power);
      return;
    }

    powerLevel = power;

    if (locked) {
      warn("motor locked");
      return;
    }
    controller.motorMove(this);
    broadcastState();
  }

  @Override
  public void moveTo(double newPos, Double powerLevel) {
    this.powerLevel = powerLevel;
    if (controller == null) {
      error(String.format("%s's controller is not set", getName()));
      return;
    }

    // targetPos = encoderMap.calc(newPos);
    targetPos = newPos;
    controller.motorMoveTo(this);
    broadcastState();
  }

  @Override
  public void moveTo(double newPos) {
    moveTo(newPos, null);
  }

  @Override
  public void setInverted(boolean invert) {
    powerMap.setInverted(invert);
    // controller.motorMove(this); - motor should not be 
    // told to move for setting
    // inverted
    broadcastState();
  }

  // ---- Servo begin ---------
  public void setMinMax(double min, double max) {
    powerMap.setMin(min);
    powerMap.setMax(max);
    broadcastState();
  }

  public void setPowerLevel(double power) {
    powerLevel = power;
  }

  @Override
  public void stop() {
    // log.info("{}.stop()", getName());
    powerLevel = 0.0;
    if (controller != null) {
      controller.motorStop(this);
    }
    broadcastState();
  }

  @Override
  public void stopAndLock() {
    // log.info("stopAndLock");
    move(0.0);
    lock();
    broadcastState();
  }

  @Override
  public void unlock() {
    // log.info("unLock");
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
  public Integer updatePosition(Integer position) {
    currentPos = position;
    return position;
  }

  @Override
  public double getTargetPos() {
    return targetPos;
  }

  @Override
  public void pulse() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setEncoder(Encoder encoder) {
    // TODO Auto-generated method stub

  }

  public void detachMotorController(MotorController controller) {
    controller.detach(this);
    controller = null;
    controllerName = null;
    broadcastState();
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (MotorController.class.isAssignableFrom(service.getClass())) {
      attachMotorController((MotorController) service);
      return;
    } 

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }
  
  public void onPin(PinData data){
    Double pwr = null;
    if (inputMapper != null){
      pwr = inputMapper.calcOutput(data.value);
    } else{
      pwr = data.value.doubleValue();
    }
    move(pwr);
  }
  
  public void onJoystickData(JoystickData data){
    Double pwr = null;
    if (inputMapper != null){
      pwr = inputMapper.calcOutput(data.value);
    } else{
      pwr = data.value.doubleValue();
    }
    move(pwr);
  }
  
  //////////////// begin new stuff ///////////////////////

  public void attach(PinDefinition pindef){
    // SINGLE PIN MAN !! not ALL PINS !
    // must be local now :P 
    // FIXME this "should" be cable of adding vi
    // e.g send(pindef.getName(), "attach", getName(), pindef.getAddress());
    // attach(pindef.getName(), pindef.getAddress)
    PinArrayControl pac = (PinArrayControl)Runtime.getService(pindef.getName());
    pac.attach(this, pindef.getAddress());
    // subscribe(pindef.getName(), "publishPin", getName(), "move");
  }
  
  public void attach(Component joystickComponent){
    send(joystickComponent.getName(), "addListener", getName(), joystickComponent.id);
  }
  
  public void attach(ButtonDefinition buttondef){
    subscribe(buttondef.getName(), "publishButton", getName(), "move");
  }

  
  //////////////// end new stuff ///////////////////////
  
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

    this.controller = controller;
    this.controllerName = controller.getName();

    broadcastState();
    controller.attach(this);
  }

  /////// config start ////////////////////////

  @Override
  public boolean isAttached() {
    return controller != null;
  }

  // TODO - this could be Java 8 default interface implementation
  @Override
  public void detach(String controllerName) {
    if (controller == null || !controllerName.equals(controller.getName())) {
      return;
    }
    controller.detach(this);
    controller = null;
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

}