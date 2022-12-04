package org.myrobotlab.kinematics;

public class PoseSequence {

  public int id;

  /**
   * name of pose
   */
  public String name;

  /**
   * number of ms to wait before starting this pose
   */
  public Long waitTimeMs;

  public PoseSequence() {
  }

  @Override
  public String toString() {
    if (waitTimeMs == null) {
      return name;
    } else {
      return String.format("%s %d ms");
    }
  }
}
