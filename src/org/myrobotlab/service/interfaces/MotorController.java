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

import org.myrobotlab.framework.MRLException;
import org.myrobotlab.service.Motor;

public interface MotorController extends NameProvider, MicrocontrollerPeripheral {

	
	/**
	 * typed motorAttach, typed for a reason - the Motor's attach should always be used
	 * any of the following methods expect the Motor to be valid and filled with the
	 * appropriate data by the time it's "attached" to the motor controller, so....
	 * the only parameter needed is the motor
	 * 
	 * @param motor
	 * @throws MRLException
	 */
	public void motorAttach(Motor motor) throws Exception;
	
	// ========  new interface end ===================

	
	/**
	 * MotorDetach - detach the Motor from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Motor
	 * @return void
	 */
	public boolean motorDetach(Motor motor);

	/**
	 * 
	 * request for motor to move the motor can be queried for the new powerlevel
	 * and the controller shall appropriately change power level and direction
	 * if necessary
	 * 
	 * @param name
	 */
	public void motorMove(Motor motor);

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
	public void motorMoveTo(Motor motor);

	/**
	 * stops the motor
	 * @param motor
	 */
	public void motorStop(Motor motor);
	
	/**
	 * method for resetting all the variables of a motor
	 * this will reset counters if the motor is a stepper
	 * and / or other variables for other sorts of motors
	 * 
	 * @param motor
	 */
	public void motorReset(Motor motor);
	
	/**
	 * tests if this controller is connected & ready
	 * @return
	 */
	public boolean isConnected();


}
