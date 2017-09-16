/**
 * 
 */
package org.myrobotlab.kinematics;

/**
 * @author chris
 *
 */
public class PositionData {
  public String armName;
  public Point position;
  public PositionData(String armName, Point position) {
    this.armName = armName;
    this.position = position;
  }

}
