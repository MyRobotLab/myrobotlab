package org.myrobotlab.jme3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.VirtualServoController;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

public class Jme3ServoController implements ServoController {

  JMonkeyEngine jme = null;

  public final static Logger log = LoggerFactory.getLogger(VirtualServoController.class);

  Map<String, ServoData> servos = new TreeMap<String, ServoData>();
  Map<String, String[]> multiMapped = null;

  class ServoData {

    ServoControl servo;
    UserData data;

    public ServoData(ServoControl servo, UserData data) {
      this.servo = servo;
      this.data = data;
    }
  }

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
      // FIXME - mapping
      UserData data = jme.getUserData(name);
      if (data == null) {
        log.error("attachServoControl - cannot find node for servo {}", name);
      }
      servos.put(name, new ServoData(servo, data));
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
      velocity = 20;
    }

    // jme.getAttached()

    String[] multi = multiMapped.get(name);
    if (multi != null) {
      for (String nodeName : multi) {
        jme.rotateTo(nodeName, servo.getPos(), velocity);
      }
    } else {
      jme.rotateTo(name, servo.getPos(), velocity);
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

}
