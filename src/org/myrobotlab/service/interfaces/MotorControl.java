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

import org.myrobotlab.motor.MotorConfig;
import org.myrobotlab.sensor.Encoder;

public interface MotorControl extends DeviceControl, RelativePositionControl {

	public void attach(MotorController controller) throws Exception;

	public void detach(MotorController controller);

	double getPowerLevel();

	public void setPowerLevel(double power);

	double getPowerOutput();

	double getTargetPos();

	/**
	 * query the motor as to its inverted status
	 * 
	 * @return
	 */
	boolean isInverted();

	/**
	 * locks the motor so no other commands will affect it until it becomes
	 * unlocked
	 */
	void lock();

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * @param newPos
	 */
	void moveTo(double newPos);

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * @param newPos
	 */
	void moveTo(double newPos, Double power);

	void setEncoder(Encoder encoder);

	/**
	 * change the motors direction such that negative power levels become
	 * clockwise if previous levels were counter clockwise and positive power
	 * levels would become counter clockwise
	 * 
	 * @param invert
	 */
	void setInverted(boolean invert);

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

	public MotorConfig getConfig();

	/**
	 * testing if a 'specific' motor controller is attached
	 * @param controller
	 * @return
	 */
	boolean isAttached(MotorController controller);

	/**
	 * general test if the motor is ready without having to supply
	 * the specific motor controller
	 * @return
	 */
	boolean isAttached();
}
