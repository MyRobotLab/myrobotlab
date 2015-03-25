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

import java.util.ArrayList;

import org.myrobotlab.service.data.Pin;

public interface MotorController {

	public String getName();

	/**
	 * This is basic information to request from a Controller. A list of pins on
	 * the controller so GUIs or other services can figure out if there are any
	 * appropriate
	 * 
	 * @return
	 */
	public ArrayList<Pin> getPinList();

	/**
	 * Remote attachment activation - used by services not in the same instance
	 * to attach a Motor to a MotorController
	 * 
	 * @param motorName
	 * @param motorData
	 */
	// public boolean motorAttach(String motorName, Object... motorData);

	public boolean motorAttach(String motorName, Integer pwmPin, Integer dirPin);

	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin);

	public boolean motorAttach(String motorName, String type, Integer pwmPin, Integer dirPin, Integer encoderPin);

	/**
	 * MotorDetach - detach the Motor from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Motor
	 * @return void
	 */
	public boolean motorDetach(String name);

	/**
	 * 
	 * request for motor to move the motor can be queried for the new powerlevel
	 * and the controller shall appropriately change power level and direction
	 * if necessary
	 * 
	 * @param name
	 */
	public void motorMove(String name);

	/**
	 * moveTo - move the Motor a relative amount the amount can be negative or
	 * positive an integer value is expected
	 * 
	 * @param name
	 *            - name of the Motor
	 * @param position
	 *            - positive or negative absolute amount to move the Motor
	 * @return void
	 */
	public void motorMoveTo(String name, double position);

}
