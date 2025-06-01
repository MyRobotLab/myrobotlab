package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.List;

/** represent a set of servo positions at a given point in time */
public class Gesture {

  /**
   * sequence of poses and offset times
   */
  public List<Action> actions = new ArrayList<>();

}
