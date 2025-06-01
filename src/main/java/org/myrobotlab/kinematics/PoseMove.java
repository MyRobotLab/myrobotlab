package org.myrobotlab.kinematics;

/**
 * A move to a position at a given speed.  If the position of null, its
 * still possible to change the speed.  If the speed is null, its possible 
 * to use the "current" speed, whatever that is.
 * 
 * @author GroG
 *
 */
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
