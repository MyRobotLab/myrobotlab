package org.myrobotlab.kinematics;

import java.util.Map;
import java.util.TreeMap;

/** represent a set of servo positions at a given point in time */
public class Pose {

  public Map<String, PoseMove> moves = new TreeMap<>();

}
