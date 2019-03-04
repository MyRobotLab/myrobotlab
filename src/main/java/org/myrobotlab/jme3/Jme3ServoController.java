package org.myrobotlab.jme3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
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
      attachServoControl((ServoControl) service);
    }
  }

  @Override
  public void attach(String serviceName) throws Exception {
    // TODO Auto-generated method stub

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

  @Override
  public void attachServoControl(ServoControl servo) throws Exception {
    String name = servo.getName();
    if (!servos.containsKey(name)) {      
      servos.put(name, servo);
      servo.attach(this);
    }
  }

  // FIXME - this should probably be deprecated in the interface !
  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    attachServoControl(servo);
  }

  @Override
  public void servoAttachPin(ServoControl servo, Integer pin) {
    try {
      attachServoControl(servo);
    } catch (Exception e) {
      log.error("servoAttachPin threw", e);
    }
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
    float velocity = (float) servo.getVelocity();
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
  public void servoDetachPin(ServoControl servo) {
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

  @Override
  public void enablePin(Integer sensorPin, Integer i) {
    // TODO Auto-generated method stub

  }

  @Override
  public void disablePin(Integer i) {
    // TODO Auto-generated method stub

  }

  public void setRotation(String name, String axis) {
    rotationMap.put(name, axis);
  }

  public void setDefaultServoSpeed(Double speed) {
    defaultServoSpeed = speed;
  }

}
