package org.myrobotlab.kinematics;

public class PoseMove {
  
  public PoseMove() {    
  }
  
  public PoseMove(double position, Double speed) {
    this.position = position;
    this.speed = speed;
  }

  /**
   * frame position
   */
  public Double position;
  
  /**
   * frame speed
   */
  public Double speed;
  
}
