/**
 *                    
 * @author grog (at) myrobotlab.org
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

import java.util.List;

import org.myrobotlab.framework.interfaces.Attachable;

public interface MotorController extends Attachable {

		
	/**
	 * 
	 * request for motor to move the motor can be queried for the new powerlevel
	 * and the controller shall appropriately change power level and direction
	 * if necessary
	 * @param motor the motor that will be moved
	 */
	public void motorMove(MotorControl motor);

	/**
	 * moveTo - move the MotorControl a relative amount the amount can be
	 * negative or positive an integer value is expected
	 * 
	 * param name
	 *            - name of the MotorControl
	 * param position
	 *            - positive or negative absolute amount to move the
	 *            MotorControl
   * @param motor the motor that will be moved
	 */
	public void motorMoveTo(MotorControl motor);

	/**
	 * stops the motor
   * @param motor the motor that will be stopped
	 * 
	 */
	public void motorStop(MotorControl motor);

	/**
	 * method for resetting all the variables of a motor this will reset
	 * counters if the motor is a stepper and / or other variables for other
	 * sorts of motors
   * @param motor the motor that will be reset
	 * 
	 */
	public void motorReset(MotorControl motor);
	
	/**
	 * if the motor controller uses ports - this method will return a list of ports
	 * @return
	 */
	List<String> getPorts();

}
