package org.myrobotlab.jme3;

import java.util.Set;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;

import com.jme3.scene.Node;

public class Jme3ServoController extends Jme3Object implements ServoController {

  public Jme3ServoController(Node node) {
    super(node);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void attach(Attachable service) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(String serviceName) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void detach(Attachable service) {
    // TODO Auto-generated method stub

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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void attachServoControl(ServoControl servo) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoAttachPin(ServoControl servo, int pin) {
    // TODO Auto-generated method stub

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
    // TODO Auto-generated method stub

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
