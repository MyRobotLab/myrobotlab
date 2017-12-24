package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface RelativePositionControl extends NameProvider {
	
	/**
	 * Move is the most common motor command. The command accepts a parameter of
	 * power which can be of the range -1.0 to 1.0. Negative values are in one
	 * direction and positive values are in the opposite value. For example -1.0
	 * would be maximum power in a counter clock-wise direction and 0.9 would be
	 * 90% power in a clockwise direction. 0.0 of course would be stop
	 * 
	 * @param power
	 *            - new power level
	 */
	void move(double power);
	
	// FIXME - in another interface Remotable ?
	boolean isLocal();

}
