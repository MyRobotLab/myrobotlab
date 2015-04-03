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

import org.myrobotlab.service.Stepper;
import org.myrobotlab.service.data.Pin;

public interface StepperController {

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
	 * set the stepper speed
	 * @param speed
	 */
	public void setStepperSpeed(Integer speed);

	/**
	 * attach stepper to controller via reference
	 * @param stepper
	 * @return
	 */
	public boolean stepperAttach(Stepper stepper);

	/**
	 * attach stepper to control by name
	 * @param stepperName
	 * @return
	 */
	public boolean stepperAttach(String stepperName);

	/**
	 * detatch the stepper from the controller - reset resources
	 * @param name
	 * @return
	 */
	public boolean stepperDetach(String name);

	/**
	 * method to return stepper information
	 * 
	 * @param stepperName
	 * @return
	 */
	// public Object[] getStepperData(String stepperName);

	public void stepperReset(String stepper);

	/**
	 * the basic stepper motor movement control
	 * 
	 * @param name
	 * @param pos
	 * @param style
	 */
	public void stepperMoveTo(String name, int pos, int style);

	/**
	 * immediate stop command
	 * 
	 * @param name
	 */
	public void stepperStop(String name);

}
