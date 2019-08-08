/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

/**
 * @author calamity
 *
 */
public enum ArmConfig {
	DEFAULT,
	REVERSE,  // origin set a end point and end of arm will set the origin of the other arms, needs to define it's own DH params.
	IMMOBILE, // IK engine won't move that arm, but it may still be move by other mean and will move if it's origin move
	HOLD_POSITION // the end point of the arm will try to keep it's position when the rest of the body moves
}
