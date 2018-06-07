package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.interfaces.ServoControl;

/**
 * skeleton spare part for universal Skeleton ServoControl gestures
 * Inspired by InMoov.java
 * 
 * TODO Universal main nervous system service to control it
 */
public class Skeleton extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Skeleton.class);

  /**
   * Servos current collection for InMoovHand
   */
  transient ArrayList<ServoControl> servos = new ArrayList<ServoControl>();

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    Skeleton inMoovTorso = (Skeleton) Runtime.start("inMoovTorso", "Skeleton");

    VirtualArduino virtualArduino = (VirtualArduino) Runtime.start("virtualArduino", "VirtualArduino");
    try {
      virtualArduino.connect("COM42");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
    arduino.connect("COM42");

    // virtual arduino can't simulate velocity at this time
    // i2c service connected onto virtual arduino will do the job
    // https://github.com/MyRobotLab/myrobotlab/issues/99
    Adafruit16CServoDriver adafruit16CServoDriver = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver", "Adafruit16CServoDriver");
    adafruit16CServoDriver.attach(arduino, "0", "0x40");

    Servo servo = (Servo) Runtime.start("servo", "Servo");

    try {
      inMoovTorso.attach(servo);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    inMoovTorso.moveTo(10.0);
    inMoovTorso.moveTo(90.0, 90.0, 90.0, 90.0, 90.0);
  }

  public Skeleton(String n) {
    super(n);
  }

  public void attach(Attachable... attachable) {

    for (int i = 0; i < attachable.length; i++) {
      if (attachable[i] instanceof ServoControl) {
        if (!servos.contains((ServoControl) attachable[i])) {
          servos.add((ServoControl) attachable[i]);
          //if (servosConventionalNames.size() >= servos.size() - 1) {
          //  info(attachable.getClass().getSimpleName() + " " + attachable[i].getName() + " attached as : " + servosConventionalNames.get(servos.size() - 1));
          //}
        }
      } else {
        error("don't know how to attach a {}", attachable[i].getName());
      }
    }

  }

  /** 
   * move a group of servo
   * moveTo order is based on attach order, very important !
   * Please note the InMoov1 syntax for backward compatibility :
   * Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist
   */
  public void moveTo(Double... servoPos) {
    checkParameters(servoPos.length);
    for (int i = 0; i < servoPos.length && i < servos.size(); i++) {
      servos.get(i).moveTo(servoPos[i]);
    }
  }

  /** 
   * move a group of servo
   * And wait for every servo of the group
   * reached the asked position
   */
  public void moveToBlocking(Double... servoPos) {
    checkParameters(servoPos.length);
    log.info(String.format("init " + getName() + "moveToBlocking "));
    for (int i = 0; i < servoPos.length && i < servos.size(); i++) {
      servos.get(i).moveTo(servoPos[i]);
    }
    waitTargetPos();
    log.info(String.format("end " + getName() + "moveToBlocking "));
  }

  public void waitTargetPos() {
    for (int i = 0; i < servos.size(); i++) {
      servos.get(i).waitTargetPos();
    }
  }

  /** 
   * Detect if every servo of the group are attached to a controller
   */
  public boolean isAttached() {
    boolean attached = false;
    for (int i = 0; i < servos.size(); i++) {
      attached |= servos.get(i).isAttached();
    }
    return attached;
  }

  /** 
   * Electrize servos group
   */
  public void enable() {
    for (int i = 0; i < servos.size(); i++) {
      servos.get(i).enable();
    }
  }

  /** 
   * Shutdown servos group
   */
  public void disable() {
    for (int i = 0; i < servos.size(); i++) {
      servos.get(i).disable();
    }
  }

  public void setVelocity(Double... servoVelocity) {

    checkParameters(servoVelocity.length);
    for (int i = 0; i < servoVelocity.length && i < servos.size(); i++) {
      servos.get(i).setVelocity(servoVelocity[i]);
    }
  }

  public void setAutoDisable(Boolean... param) {
    checkParameters(param.length);
    for (int i = 0; i < param.length && i < servos.size(); i++) {
      servos.get(i).setAutoDisable(param[i]);
    }
  }

  public void setOverrideAutoDisable(Boolean... param) {

    checkParameters(param.length);
    for (int i = 0; i < param.length && i < servos.size(); i++) {
      servos.get(i).setOverrideAutoDisable(param[i]);
    }
  }

  public void rest() {
    for (int i = 0; i < servos.size(); i++) {
      servos.get(i).rest();
    }
  }

  public boolean checkParameters(Integer parameters) {
    if (parameters > servos.size()) {
      warn("Too many input parameters for " + this.getIntanceName() + " ! not enough elements, don't worry will move what is availabe ...");
      return false;
    }
    return true;
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Skeleton.class.getCanonicalName());
    meta.addDescription("An easier way to control multiple servos");
    meta.addCategory("robot");
    return meta;
  }

}
