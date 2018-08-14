package org.myrobotlab.service.abstracts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;

public abstract class AbstractMotorController extends Service implements MotorController {
  
  protected transient Map<String, AbstractMotor> motors = new HashMap<String, AbstractMotor>();

  private static final long serialVersionUID = 1L;
  
  // @Deprecated // use MotorControl's powerMapper as its "per" Motor ! - this is global :P
  // protected Mapper powerMapper = null;
  
  /**
   * default mapping and limits !
   * This will be overriden by the AbstractMotor's values (if set)
   */
  // input range
  Double minX = -1.0;
  Double maxX = 1.0;

  // output range
  Double minY = -1.0;
  Double maxY = 1.0;
  
  // min max of input
  Double minInput = null;
  Double maxInput = null;

  // min max of output
  Double minOutput = null;
  Double maxOutput = null;

  public AbstractMotorController(String reservedKey) {
    super(reservedKey);    
  }
  
  // setting default map values
  public void map(double minX, double maxX, double minY, double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    broadcastState();
  }
  
  // TODO - switch to MotorControl interface instead of AbstractMotor
  public double calcOutput(AbstractMotor mc) {
    double in = mc.getPowerLevel();
    
    // FIXME - an optimization would be to "set" the values of the MotorControl
    // if they are null "once" in the attach - so this doesn't have to be done each 
    // calc
    double minX = (mc.getMinX() == null)? this.minX:mc.getMinX();
    double maxX = (mc.getMaxX() == null)? this.maxX:mc.getMaxX();
    double minY = (mc.getMinY() == null)? this.minY:mc.getMinY();
    double maxY = (mc.getMaxY() == null)? this.maxY:mc.getMaxY();
    
    // motor control has highest precedence
    Double minInput = mc.getMinInput();
    Double maxInput = mc.getMaxInput();
    // motor controller has next order of precedence
    if (minInput == null) {
      minInput = this.minInput;
    }
    if (maxInput == null) {
      maxInput = this.maxInput;
    }

    // motor control has highest precedence
    Double minOutput = mc.getMinOutput();
    Double maxOutput = mc.getMaxOutput();
    // motor controller has next order of precedence
    if (minOutput == null) {
      minOutput = this.minOutput;
    }
    if (maxOutput == null) {
      maxOutput = this.maxOutput;
    }
    
    if (minInput != null && in < minInput) {
      log.warn("clipping input {} to {}", in, minInput);
      in = minInput;
    }
    if (maxInput != null && in > maxInput) {
      log.warn("clipping input {} to {}", in, maxInput);
      in = maxInput;
    }
    
    double c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);

    if (minOutput != null && c < minOutput) {
      log.warn("clipping output {} to {}", c, minOutput);
      return minOutput;
    }
    if (maxOutput != null && c > maxOutput) {
      log.warn("clipping output {} to {}", c, maxOutput);
      return maxOutput;
    }
    return c;
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
  public void attach(Attachable motor) throws Exception {
    
    // check if service already attached
    if (isAttached(motor)) {
      log.info("{} is attached to {}", motor.getName(), getName());
      return;
    }

    // FIXME - route Motor MotorPort & MotorDualPwm to
    // motorAttach( ?? ) to be overloaded by subclass - default methods produce
    // "not implemented error"
    /*
    <pre>
    FIXME - test AbstractMotor(Control) - 
    if (MotorControl.class.isAssignableFrom(service.getClass())) {
      MotorControl motor = (MotorControl) service;
      String port = motor.getPort();

      if (port == null || (!ports.contains(port))) {
        throw new IOException("port number in motor must be set to m1 or m2");
      }

      motors.put(motor.getName(), motor);

      // give opportunity for motor to attach
      motor.attach(this);

      // made changes broadcast it
      broadcastState();
      return;
    }
    </pre>
    */
    // FIXME - decide what unit AbstractMotorController will use (probably AbstractMotor)
    if (MotorControl.class.isAssignableFrom(motor.getClass())) {
      motors.put(motor.getName(), (AbstractMotor)motor);
      motor.attach(this);
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), motor.getClass().getSimpleName());
  }
  
}
