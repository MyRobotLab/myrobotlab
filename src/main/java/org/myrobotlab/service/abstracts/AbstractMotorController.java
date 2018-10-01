package org.myrobotlab.service.abstracts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;

public abstract class AbstractMotorController extends Service implements MotorController {

  protected transient Map<String, AbstractMotor> motors = new HashMap<String, AbstractMotor>();

  private static final long serialVersionUID = 1L;

  MapperLinear defaultMapper;

  public AbstractMotorController(String reservedKey) {
    super(reservedKey);
  }

  // setting default map values
  public void map(double minX, double maxX, double minY, double maxY) {
    if (defaultMapper == null) {
      defaultMapper = new MapperLinear();
    }
    defaultMapper.map(minX, maxX, minY, maxY);
    // we want to setMap because we merge with motorControl values
    // which initially hold limits - and we don't want to int
    // defaultMapper.setMap(minX, maxX, minY, maxY);
    // defaultMapper.setMinMaxOutput(minY, maxY);
    broadcastState();
  }

  // TODO - switch to MotorControl interface instead of AbstractMotor
  public double motorCalcOutput(MotorControl mc) {
    double calculatedMotorControllerOutput = mc.calcControllerOutput();
    info("%s.move(%f) -> %s.motorMove %.2f -> hardware", mc.getName(), mc.getPowerLevel(), getName(), calculatedMotorControllerOutput);
    return calculatedMotorControllerOutput;
  }

  // FIXME - if kept they should be put in interface
  public boolean motorDetach(String name) {
    if (motors.containsKey(name)) {
      motors.remove(name);
      return true;
    }
    return false;
  }

  // FIXME - if kept they should be put in interface
  public void motorMove(String name) {
    motorMove((MotorControl) Runtime.getService(name));
  }

  // FIXME - if kept they should be put in interface
  public void motorMoveTo(String name, Integer position) {
    error("not implemented");
  }

  @Override
  public void motorReset(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorMoveTo(MotorControl motor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void motorStop(MotorControl motor) {
    motor.move(0);
  }

  @Override
  public void detach() {
    for (String name : motors.keySet()) {
      Motor m = (Motor) Runtime.getService(name);
      if (m != null) {
        m.detach(this);
      }
    }
  }

  // only handles motors - subclasses must use super plus
  // their own isAttached
  @Override
  public boolean isAttached(Attachable service) {
    return motors.containsKey(service.getName());
  }

  @Override
  public Set<String> getAttached() {
    return motors.keySet();
  }

  public void detach(MotorControl device) {
    motors.remove(device);
  }

  /**
   * Routing Attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable service) throws Exception {

    // check if service already attached
    if (isAttached(service)) {
      log.info("{} is attached to {}", service.getName(), getName());
      return;
    }

    // FIXME - route Motor MotorPort & MotorDualPwm to
    // motorAttach( ?? ) to be overloaded by subclass - default methods produce
    // "not implemented error"
    /*
     * <pre> FIXME - test AbstractMotor(Control) - if
     * (MotorControl.class.isAssignableFrom(service.getClass())) { MotorControl
     * motor = (MotorControl) service; String port = motor.getPort();
     * 
     * if (port == null || (!ports.contains(port))) { throw new
     * IOException("port number in motor must be set to m1 or m2"); }
     * 
     * motors.put(motor.getName(), motor);
     * 
     * // give opportunity for motor to attach motor.attach(this);
     * 
     * // made changes broadcast it broadcastState(); return; } </pre>
     */
    // FIXME - decide what unit AbstractMotorController will use (probably
    // AbstractMotor)
    if (MotorControl.class.isAssignableFrom(service.getClass())) {
      MotorControl motor = (MotorControl) service;
      // add motor to set of current motors this controller is attached to
      motors.put(motor.getName(), (AbstractMotor) motor);

      // give opportunity for motor to attach
      motor.attach(this);
      broadcastState();
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  public void connect(String port) throws Exception {
    log.warn("AbstractMotorController connect not implemented");
  }

  @Override
  public Mapper getDefaultMapper() {
    return defaultMapper;
  }

}
