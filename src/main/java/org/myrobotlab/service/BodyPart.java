package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractBodyPart;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * Body spare parts for universal ServoControl gestures Inspired by
 * InMoov.java...
 * 
 * TODO : IK moveTo(x,y,z) ?
 * 
 * Syntax to declare body part Runtime.start("nodeToAttach.name", "BodyPart");
 * 
 * Syntax to declare an actuator Runtime.start("nodeToAttach.name", "Servo");
 * 
 * 
 * Do not declare the whole path i01.rightarm.righthand.thumb... as name , just
 * the node to attach : The root will learn every nodes attached for a complete
 * linkage
 * 
 */
public class BodyPart extends AbstractBodyPart {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(BodyPart.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    BodyPart rightArm = (BodyPart) Runtime.start("rightArm", "BodyPart");
    BodyPart rightHand = (BodyPart) Runtime.start("rightHand", "BodyPart");
    Runtime.start("gui", "SwingGui");
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

    Servo servo = (Servo) Runtime.start("rightArm.bicep", "Servo");
    Servo servo2 = (Servo) Runtime.start("rightArm.rotate", "Servo");
    Servo servo3 = (Servo) Runtime.start("rightHand.majeure", "Servo");
    Servo servo4 = (Servo) Runtime.start("rightHand.thumb", "Servo");

    try {
      servo.attach(adafruit16CServoDriver, 1);
      servo2.attach(adafruit16CServoDriver, 2);
      servo3.attach(adafruit16CServoDriver, 3);
      servo4.attach(adafruit16CServoDriver, 4);
      rightArm.attach(servo, servo2);
      rightHand.attach(servo4, servo3);
      rightArm.attach(rightHand);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    rightArm.moveTo(10.0);

    rightArm.moveTo(90.0, 90.0, 90.0, 90.0, 90.0);
    // log.info(rightArm.childs.toString() + "childNodes");
    // log.info(rightArm.childs.crawlForDataStartingWith("") + "");

    // inMoovTorso.servos =
    // inMoovTorso.childs.crawlForDataStartingWith("rightArm");
    rightArm.moveTo("rightHand", 63.0);
    rightArm.getBodyParts();
  }

  public BodyPart(String n) {
    super(n);
    // optional standardised servo names for priority order
    servoOrder.put("thumb", 0);
    servoOrder.put("index", 1);
    servoOrder.put("majeure", 2);
    servoOrder.put("ringFinger", 3);
    servoOrder.put("pinky", 4);
    servoOrder.put("bicep", 5);
    servoOrder.put("rotate", 6);
    servoOrder.put("shoulder", 7);
    servoOrder.put("omoplate", 8);
    servoOrder.put("neck", 9);
    servoOrder.put("rothead", 10);
    servoOrder.put("rollNeck", 11);
    servoOrder.put("eyeX", 12);
    servoOrder.put("eyeY", 13);
    servoOrder.put("jaw", 14);
    servoOrder.put("topStom", 15);
    servoOrder.put("midStom", 16);
    servoOrder.put("lowStom", 17);
  }

  public void attach(Attachable attachable) {

    // attach the child to this node
    if (attachable instanceof BodyPart) {
      // store bodypart service inside the tree
      thisNode.put(this.getName() + "." + attachable.getName(), attachable);
      // store bodypart nodes
      ArrayList<Attachable> nodes = ((BodyPart) attachable).thisNode.flatten();
      for (Attachable service : nodes) {
        thisNode.put(this.getName() + "." + service.getName(), service);
      }

      // or ServoControl ( as leaf ) to this
    } else if (attachable instanceof ServoControl) {
      // detect if syntax name is correct : parent.child
      if (StringUtils.countMatches(attachable.getName(), ".") == 1) {
        thisNode.put(attachable.getName(), attachable);
      } else {
        error("Can't attach %s, we need {parent.child} format .", attachable.getName());
      }
    }
    broadcastState();
  }

  public void attach(ServoControl... attachable) {
    for (int i = 0; i < attachable.length; i++) {
      attach(attachable[i]);
    }
  }

  public void moveTo(Double... servoPos) {
    moveTo(this.getIntanceName(), servoPos);
  }

  public void moveToBlocking(Double... servoPos) {
    moveToBlocking(this.getIntanceName(), servoPos);
  }

  public void waitTargetPos() {
    waitTargetPos(this.getIntanceName());
  }

  /**
   * Detect if every servo of the group are attached to a controller
   */
  public boolean isAttached() {
    boolean attached = false;
    for (int i = 0; i < getAcuators(this.getIntanceName()).size(); i++) {
      attached |= getAcuators(this.getIntanceName()).get(i).isAttached();
    }
    return attached;
  }

  /**
   * Electrize servos group
   */
  public void enable() {
    for (int i = 0; i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).enable();
    }
  }

  /**
   * Shutdown power of servos group
   */
  public void disable() {
    for (int i = 0; i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).disable();
    }
  }

  public void setVelocity(Double... servoVelocity) {
    for (int i = 0; i < servoVelocity.length && i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).setVelocity(servoVelocity[i]);
    }
  }

  public void setRest(Double... rest) {
    for (int i = 0; i < rest.length && i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).setRest(rest[i]);
    }
  }

  public void setAutoDisable(Boolean... param) {
    for (int i = 0; i < param.length && i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).setAutoDisable(param[i]);
    }
  }

  public void setOverrideAutoDisable(Boolean... param) {
    for (int i = 0; i < param.length && i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).setOverrideAutoDisable(param[i]);
    }
  }

  public void rest() {
    for (int i = 0; i < getAcuators(this.getIntanceName()).size(); i++) {
      getAcuators(this.getIntanceName()).get(i).rest();
    }
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(BodyPart.class.getCanonicalName());
    meta.addDescription("An easier way to control a body ...");
    meta.addCategory("robot");
    meta.setAvailable(true);
    meta.addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    return meta;
  }
}