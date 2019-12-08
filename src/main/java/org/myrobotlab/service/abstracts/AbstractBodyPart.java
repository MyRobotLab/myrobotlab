package org.myrobotlab.service.abstracts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.BodyPart;
import org.myrobotlab.service.interfaces.ServoControl;

/**
 * Shared some methods between bodyPart and root controller ( like inmoov service
 * )
 */
public abstract class AbstractBodyPart extends Service {

  private static final long serialVersionUID = 1L;

  public transient Index<Attachable> thisNode = new Index<Attachable>();
  /**
   * System will sort servo order based first on conventional name ( if it find
   * any off it ), then by attach order.
   */
  protected HashMap<String, Integer> servoOrder = new HashMap<String, Integer>();

  public AbstractBodyPart(String reservedKey, String id) {
    super(reservedKey, id);
  }

  /**
   * get attached body parts ( parents )
   */
  public ArrayList<BodyPart> getBodyParts() {

    ArrayList<Attachable> nodes = thisNode.flatten();
    ArrayList<BodyPart> bodyParts = new ArrayList<BodyPart>();
    for (Attachable service : nodes) {
      if (service instanceof BodyPart) {
        bodyParts.add((BodyPart) service);
        log.info(service + " found by getChilds");
      }
    }
    return bodyParts;
  }

  /**
   * get a ServoControl element inside the branches by name
   */
  public ServoControl getServo(String identifier) {
    return (ServoControl) thisNode.getNode(thisNode.findNode(identifier)).getValue();
  }

  /**
   * get a BodyPart element inside the branches by name
   */
  public BodyPart getBodyPart(String identifier) {
    return (BodyPart) thisNode.getNode(thisNode.findNode(identifier)).getValue();
  }

  /**
   * move a group of servo moveTo order is based on attach order ! But ... If
   * you use some dedicated conventional names for your servo, like
   * rightHand.thumb ... This standardized order will be respected
   * 
   * Please note syntax order for information : HAND thumb, index, majeure,
   * ringFinger, pinky, wrist ARM bicep, rotate, shoulder, omoplate HEAD neck,
   * rothead, rollNeck, eyeX, eyeY, jaw
   */
  public void moveTo(String node, Double... servoPos) {
    checkParameters(node, servoPos.length);
    info("moveTo %s servo from %s node that contain %s servo", servoPos.length, node, getAcuators(node).size());
    for (int i = 0; i < servoPos.length && i < getAcuators(node).size(); i++) {
      getAcuators(node).get(i).moveTo(servoPos[i]);
      info("Moving %s servo", getAcuators(node).get(i));
    }
  }

  /**
   * move a group of servo And wait for every servo of the group reached the
   * asked position
   */
  public void moveToBlocking(String node, Double... servoPos) {
    checkParameters(node, servoPos.length);
    log.info("init {} moveToBlocking ", node);
    for (int i = 0; i < servoPos.length && i < getAcuators(node).size(); i++) {
      getAcuators(node).get(i).moveTo(servoPos[i]);
    }
    waitTargetPos(node);
    log.info("end {} moveToBlocking ", node);
  }

  public void waitTargetPos(String node) {
    for (int i = 0; i < getAcuators(node).size(); i++) {
      getAcuators(node).get(i).waitTargetPos();
    }
  }

  public boolean checkParameters(String node, Integer parameters) {
    if (parameters > getAcuators(node).size()) {
      warn("Too many input parameters for " + node + " ! not enough elements, don't worry will move what is availabe ...");
      return false;
    }
    return true;
  }

  /**
   * @return childsServo : the servos attached to the bodyPart
   */
  public ArrayList<ServoControl> getAcuators(String bodyPart) {

    ArrayList<String> leafs = thisNode.getLeafs(thisNode.findNode(bodyPart));

    // Iterate over the desired node to get only ServoControl elements

    /**
     * temporary Servo list collection to get priority by conventional names
     */
    ArrayList<ServoControl> childsServoTmp = new ArrayList<ServoControl>();
    ArrayList<ServoControl> childsServo = new ArrayList<ServoControl>();
    HashMap<Integer, ServoControl> childsServoHash = new HashMap<Integer, ServoControl>();

    for (String leaf : leafs) {
      Attachable service = thisNode.getNode(leaf).getValue();
      if (service instanceof ServoControl) {
        String[] leafSingleNameSplited = service.getName().split("\\.");
        String leafSingleName = leafSingleNameSplited[leafSingleNameSplited.length - 1].toLowerCase();
        // we found a standardized servo name !
        // we need a dedicated position for it ...
        if (servoOrder.containsKey(leafSingleName)) {

          childsServoHash.put(servoOrder.get(leafSingleName), (ServoControl) service);

          log.debug("Standardized servo {} found ! Set into dedicated position {}...", leafSingleName, servoOrder.get(leafSingleName));
        } else {
          childsServoTmp.add((ServoControl) service);
        }

      }
    }
    // sort standardized servo list first
    Map<Integer, ServoControl> sortedServo = new TreeMap<Integer, ServoControl>(childsServoHash);
    childsServo.addAll(sortedServo.values());
    // add unknown type
    childsServo.addAll(childsServoTmp);
    return childsServo;
  }
}