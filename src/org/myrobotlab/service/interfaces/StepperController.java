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

public interface StepperController {

	public final Integer STYLE_SINGLE = 1;
	public final Integer STYLE_DOUBLE = 2;
	public final Integer STYLE_INTERLEAVE = 3;
	public final Integer STYLE_MICROSTEP = 4;
	
	
	/**
	 * attach a stepper named to this controller
	 * @param stepperName
	 * @param steps
	 * @param pin1
	 * @param pin2
	 * @param pin3
	 * @param pin4
	 * @return
	 */
	public boolean stepperAttach(String stepperName, Integer steps, Object...data); 
	
	
	/**
	 * typed attachment 
	 * @param stepper
	 * @param steps
	 * @param pin1
	 * @param pin2
	 * @param pin3
	 * @param pin4
	 * @return
	 */
	public boolean stepperAttach(StepperControl stepper, Integer steps, Object...data);

	/**
	 * This is basic information to request from a Controller. A list of pins on
	 * the controller so GUIs or other services can figure out if there are any
	 * appropriate
	 * 
	 * @return
	 */
	public ArrayList<Pin> getPinList();

	/**
	 * stepperStep - move stepper an increment 
	 * 
	 * @param name
	 *            - name of the Stepper
	 * @param position
	 *            - positive to turn one direction, negative to turn the other
	 * @return void
	 */
	public void stepperStep(String name, Integer steps);

	/**
	 * stepperStep - move stepper an increment 
	 * 
	 * @param name
	 * @param steps
	 * @param style - style of stepping STYLE_SINGLE STYLE_DOUBLE STYLE_INTERLEAVE STYLE_MICROSTEP
	 */
	public void stepperStep(String name, Integer steps, Integer style);
	
	public void setSpeed(Integer speed);
	
	
	/**
	 * StepperDetach - detach the Stepper from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Stepper
	 * @return void
	 */
	public boolean stepperDetach(String name);

	public String getName();

	/**
	 * method to return stepper information
	 * 
	 * @param stepperName
	 * @return
	 */
	public Object[] getStepperData(String stepperName);

}
