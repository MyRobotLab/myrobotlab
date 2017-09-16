/**
 * 
 */
package org.myrobotlab.kinematics;

import org.myrobotlab.kinematics.Map3D.CoordStateValue;

/**
 * @author Christian
 *
 */
public class Map3DPoint {
	CoordStateValue value = CoordStateValue.UNDEFINED;
	boolean usedForObject = false;
	public Point point;
}
