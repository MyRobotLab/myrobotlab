package org.myrobotlab.jme3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

public class Jme3ServoController implements ServoController {

  JMonkeyEngine jme = null;

  public final static Logger log = LoggerFactory.getLogger(Jme3ServoController.class);

  Map<String, ServoControl> servos = new TreeMap<String, ServoControl>();
  Map<String, String[]> multiMapped = null;
  @Deprecated // remove - this should be done by creating a "new" Node .. e.g. jme.addNode(x)
  Map<String, String> rotationMap = new TreeMap<String, String>();

  double defaultServoSpeed = 60;

  public Jme3ServoController(JMonkeyEngine jme) {
    this.jme = jme;
    this.multiMapped = jme.getMultiMapped();
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (service instanceof ServoControl) {
      ServoControl servo = (ServoControl)service;
      String name = servo.getName();
      if (!servos.containsKey(name)) {      
        servos.put(name, servo);
        servo.attach(this);
      }      
    }
  }

  @Override
  public void attach(String serviceName) throws Exception {
    attach(Runtime.getService(serviceName));
  }

  @Override
  public void detach(Attachable service) {
    servos.remove(service.getName());
  }

  @Override
  public void detach(String serviceName) {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach() {
    // TODO Auto-generated method stub

  }

  @Override
  public Set<String> getAttached() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAttached(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isLocal() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getName() {
    return jme.getName();
  }

  // FIXME - this should probably be deprecated in the interface !
  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    attach(servo);
  }

  @Override
  public void servoSweepStart(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoMoveTo(ServoControl servo) {
    String name = servo.getName();
    if (!servos.containsKey(name)) {
      log.error("servoMoveTo({})", servo);
      return;
    }
    double velocity = servo.getSpeed();
    if (velocity == -1) {
      velocity = (float)defaultServoSpeed;
    }

    String axis = rotationMap.get(name);

    String[] multi = multiMapped.get(name);
    if (multi != null) {
      for (String nodeName : multi) {
        jme.rotateOnAxis(nodeName, axis, servo.getPos(), velocity);
      }
    } else {
      jme.rotateOnAxis(name, axis, servo.getPos(), velocity);
    }
  }

  @Override
  public void servoWriteMicroseconds(ServoControl servo, int uS) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetVelocity(ServoControl servo) {
    // TODO Auto-generated method stub
  }

  @Override
  public void servoSetAcceleration(ServoControl servo) {
    // TODO Auto-generated method stub

  }


  public void setRotation(String name, String axis) {
    rotationMap.put(name, axis);
  }

  public void setDefaultServoSpeed(Double speed) {
    defaultServoSpeed = speed;
  }

  @Override // FIXME - enable/disable
  public void servoEnable(ServoControl servo) {
    // TODO Auto-generated method stub
    
  }

  @Override // FIXME - enable disable
  public void servoDisable(ServoControl servo) {
    // TODO Auto-generated method stub
    
  }

}
