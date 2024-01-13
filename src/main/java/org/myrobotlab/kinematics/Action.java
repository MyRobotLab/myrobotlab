package org.myrobotlab.kinematics;

import java.util.Map;

public class Action {
  // consider enum type
  enum Type {
    moveTo, speak, sleep, message
  }

  /**
   * type determines how to handle the value depending on what is desired ...
   */
  public String type = "moveTo"; // moveTo | speak | sleep | Message

  /**
   * delay type when type is Delay, String when type is Text
   */
  public Object value;

  /**
   * blocks if true - and will wait for this action to complete before going to
   * the next action
   */
  public boolean willBlock = false;

  @Override
  public String toString() {
    return String.format("part %s %s", type, (value != null) ? value.toString() : null);
  }

  public static Action createMoveToAction(Map<String, Map<String, Object>> moves) {
    Action action = new Action();
    action.type = "moveTo";
    // TODO - check validity of moves 
    action.value = moves;
    return action;
  }
  
  public static Action createSleepAction(double sleep) {
    Action action = new Action();
    action.type = "sleep";
    action.value = sleep;
    return action;
  }

  public static Action createSpeakAction(Map<String, Object> speechCommand) {
    Action action = new Action();
    action.type = "speak";
    action.value = speechCommand;
    return action;
  }

  public static Action createGestureToAction(String gestureName) {
    Action action = new Action();
    action.type = "gesture";
    action.value = gestureName;
    return action;
  }
}
