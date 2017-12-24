/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.sensor.Encoder;

public interface MotorControl extends NameProvider, RelativePositionControl, Attachable {
  

  void attachMotorController(MotorController controller) throws Exception;

	void detachMotorController(MotorController controller);

	/**
	 * the raw non-computed input 
	 * range is -1.0 <---> 1.0
	 * @return input range
	 */
	double getPowerLevel();

	/**
	 * grog says, BAD METHOD - needs to be a solid
	 * range interface between MotorControl & MotorControllers
	 * where the range can be guaranteed -1.0 --to-- 1.0
	 * it's MotorControllers job to change this if needed into
	 * specific values (not MotorControl's job)
	 * 
	 * the 'computed' power output
	 * range can be anything ?
	 * @return computer power output
	 */
	// double getPowerOutput(); NO NO NO !!!

	double getTargetPos();

	/**
	 * general test if the motor is ready without having to supply
	 * the specific motor controller
	 * @return true/false
	 */
	boolean isAttached();

	/**
	 * testing if a 'specific' motor controller is attached
	 * @param controller c
	 * @return true if the contorller is attached to this control.
	 */
	boolean isAttached(MotorController controller);

	/**
	 * @return query the motor as to its inverted status
	 */
	boolean isInverted();

	/**
	 * locks the motor so no other commands will affect it until it becomes
	 * unlocked
	 */
	void lock();
	
	boolean isLocked();

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * FIXME - part of AbsolutePosition interface
	 * 
	 * @param newPos the new position to move to
	 */
	void moveTo(double newPos);

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * @param newPos new position
	 * @param power 0-1
	 */
	void moveTo(double newPos, Double power);

	void setEncoder(Encoder encoder);

	/**
	 * change the motors direction such that negative power levels become
	 * clockwise if previous levels were counter clockwise and positive power
	 * levels would become counter clockwise
	 * 
	 * @param invert true or false
	 */
	void setInverted(boolean invert);

	void setPowerLevel(double power);

	/**
	 * stop the motor
	 */
	void stop();

	/**
	 * a safety mechanism - stop and lock will stop and lock the motor no other
	 * commands will affect the motor until it is "unlocked"
	 */
	void stopAndLock();

	/**
	 * unlocks the motor, so other commands can affect it
	 */
	void unlock();
}
