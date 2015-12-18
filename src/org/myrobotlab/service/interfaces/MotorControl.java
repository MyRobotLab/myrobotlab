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

import org.myrobotlab.sensor.Encoder;

public interface MotorControl {

	/**
	 * detaches the motor from the motor controller
	 * 
	 * @return
	 */
	public boolean detach();

	public String getName();

	/**
	 * get the current power level of the motor
	 * 
	 * @return
	 */
	public double getPowerLevel();

	/**
	 * reports if a motor is attached to a motor controller
	 */
	public boolean isAttached();

	/**
	 * query the motor as to its inverted status
	 * 
	 * @return
	 */
	public boolean isInverted();

	/**
	 * locks the motor so no other commands will affect it until it becomes
	 * unlocked
	 */
	public void lock();

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
	public void move(double power);

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * @param newPos
	 */
	public void moveTo(int newPos);

	/**
	 * moveTo moves the motor to a specific location. Typically, an encoder is
	 * needed in order to provide feedback data
	 * 
	 * @param newPos
	 */
	public void moveTo(int newPos, Double power);

	/**
	 * Attach a motor controller to the motor. The motor and motor controller
	 * "should be in the same instance of MRL and this reference to another
	 * service should be ok.
	 * 
	 * The motor controller uses this method to pass a reference of itself to
	 * the motor, to be used directly
	 */
	public void setController(MotorController controller);
	
	
	public void setEncoder(Encoder encoder);

	/**
	 * change the motors direction such that negative power levels become
	 * clockwise if previous levels were counter clockwise and positive power
	 * levels would become counter clockwise
	 * 
	 * @param invert
	 */
	public void setInverted(boolean invert);

	public void stop();

	/**
	 * a safety mechanism - stop and lock will stop and lock the motor no other
	 * commands will affect the motor until it is "unlocked"
	 */
	public void stopAndLock();

	/**
	 * unlocks the motor, so other commands can affect it
	 */
	public void unlock();

	/**
	 * a safety limit
	 * 
	 * @param max
	 */
	// public void setMaxPower(float max);
	
	boolean hasSensor();
	
	int[] getControlPins();
	
	String getType();
	String[] getTypes();

	double getPowerOutput();
	
	public void attach(String controllerName) throws Exception;
	
	public void attach(MotorController uC) throws Exception;

}
