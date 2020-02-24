package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.slf4j.Logger;
import org.myrobotlab.service.abstracts.AbstractBodyPart;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.ServoControl;

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
public class BodyPart extends AbstractBodyPart implements IKJointAngleListener {

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

  public BodyPart(String n, String id) {
    super(n, id);
    // optional standardised servo names for priority order
    servoOrder.put("thumb", 0);
    servoOrder.put("index", 1);
    servoOrder.put("majeure", 2);
    servoOrder.put("ringFinger", 3);
    servoOrder.put("pinky", 4);
    servoOrder.put("wrist",5);
    servoOrder.put("bicep", 6);
    servoOrder.put("rotate", 7);
    servoOrder.put("shoulder", 8);
    servoOrder.put("omoplate", 9);
    servoOrder.put("neck", 10);
    servoOrder.put("rothead", 11);
    servoOrder.put("rollNeck", 12);
    servoOrder.put("eyeX", 13);
    servoOrder.put("eyeY", 14);
    servoOrder.put("jaw", 15);
    servoOrder.put("topStom", 16);
    servoOrder.put("midStom", 17);
    servoOrder.put("lowStom", 18);
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
      // getAcuators(this.getIntanceName()).get(i).setOverrideAutoDisable(param[i]);
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

  // pasted from inmoov service, for compatibility
  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    // todo implement other body parts
    if (this.getIntanceName().toLowerCase().contains("arm")) {
      // We should walk though our list of servos and see if
      // the map has it.. if so .. move to it!
      // Peers p = InMoovArm.getPeers(getName()).getPeers("Servo");
      // TODO: look up the mapping for all the servos in the arm.

      // we map the servo 90 degrees to be 0 degrees.
      HashMap<String, Double> phaseShiftMap = new HashMap<String, Double>();
      // phaseShiftMap.put("omoplate", 90);
      // Harry's omoplate is +90 degrees from Gaels InMoov..
      // These are for the encoder offsets.
      // these map between the reference frames of the dh model & the actual
      // arm.
      // (calibration)
      phaseShiftMap.put("omoplate", 90.0);
      phaseShiftMap.put("shoulder", 90.0);
      phaseShiftMap.put("rotate", -450.0);
      phaseShiftMap.put("bicep", 90.0);

      HashMap<String, Double> gainMap = new HashMap<String, Double>();
      gainMap.put("omoplate", 1.0);
      gainMap.put("shoulder", -1.0);
      gainMap.put("rotate", -1.0);
      gainMap.put("bicep", -1.0);

      ArrayList<String> servos = new ArrayList<String>();
      servos.add("omoplate");
      servos.add("shoulder");
      servos.add("rotate");
      servos.add("bicep");
      for (String s : servos) {
        if (angleMap.containsKey(s)) {
          if ("omoplate".equals(s)) {
            Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
            if (angle < 0) {
              angle += 360;
            }
            getServo("omoplate").moveTo(angle);
          }
          if ("shoulder".equals(s)) {
            Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
            if (angle < 0) {
              angle += 360;
            }
            getServo("shoulder").moveTo(angle);
          }
          if ("rotate".equals(s)) {
            Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
            if (angle < 0) {
              angle += 360;
            }
            getServo("rotate").moveTo(angle);
          }
          if ("bicep".equals(s)) {
            Double angle = (gainMap.get(s) * angleMap.get(s) + phaseShiftMap.get(s)) % 360.0;
            getServo("bicep").moveTo(angle);
            if (angle < 0) {
              angle += 360;
            }
          }
        }
      }
    } else {
      log.warn("Kinematics class not yet implemented for this body part");
    }
  }

  // pasted from inmoov service, for compatibility
  public DHRobotArm getDHRobotArm() {
    if (this.getIntanceName().toLowerCase().contains("arm")) {
      // TODO: specify this correctly and document the reference frames!
      DHRobotArm arm = new DHRobotArm();
      // d , r, theta , alpha

      // TODO: the DH links should take into account the encoder offsets and
      // calibration maps
      DHLink link1 = new DHLink("omoplate", 0, 40, MathUtils.degToRad(-90), MathUtils.degToRad(-90));
      // dh model + 90 degrees = real
      link1.setMin(MathUtils.degToRad(-90));
      link1.setMax(MathUtils.degToRad(0));

      // -80 vs +80 difference between left/right arm.
      DHLink link2 = new DHLink("shoulder", -80, 0, MathUtils.degToRad(90), MathUtils.degToRad(90));
      // TODO: this is actually 90 to -90 ? validate if inverted.
      // this link is inverted :-/
      link2.setMin(MathUtils.degToRad(-90));
      link2.setMax(MathUtils.degToRad(90));

      DHLink link3 = new DHLink("rotate", 280, 0, MathUtils.degToRad(0), MathUtils.degToRad(90));
      // TODO: check if this is inverted. i think it is.
      link3.setMin(MathUtils.degToRad(0));
      link3.setMax(MathUtils.degToRad(180));

      DHLink link4 = new DHLink("bicep", 0, 280, MathUtils.degToRad(90), MathUtils.degToRad(0));
      // TODO: this is probably inverted? should be 90 to 0...
      link4.setMin(MathUtils.degToRad(90));
      link4.setMax(MathUtils.degToRad(180));

      arm.addLink(link1);
      arm.addLink(link2);
      arm.addLink(link3);
      arm.addLink(link4);

      return arm;
    } else {
      log.warn("Kinematics class not yet implemented for this body part");
      return null;
    }
  }
}
