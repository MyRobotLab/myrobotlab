package org.myrobotlab.service.interfaces;

public interface AbsolutePositionControl {

	/**
	 * Moves the a specific location. Typically, for example, a servo has 0 to 180
	 * positions - each increment corresponding to a degree
	 * @param newPos the new position to move to.
	 * 
	 */
	public void moveTo(double newPos);

}
