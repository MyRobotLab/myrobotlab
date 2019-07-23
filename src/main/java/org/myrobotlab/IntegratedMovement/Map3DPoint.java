/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import org.myrobotlab.IntegratedMovement.Map3D.CoordStateValue;
import org.myrobotlab.kinematics.Point;

/**
 * @author Christian
 *
 */
public class Map3DPoint {
  CoordStateValue value = CoordStateValue.UNDEFINED;
  boolean usedForObject = false;
  public Point point;
}
