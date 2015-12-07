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

public interface ServoControl {

	// FIXME - add sweep and other fun methods
	// extend Motor ???
	/**
	 * re-attaches servo to controller it was last attached to
	 * 
	 * @return
	 */
	public boolean attach();

	public boolean attach(String controller, Integer pin);

	/**
	 * detaches the servo from the controller
	 * 
	 * @return
	 */
	public boolean detach();

	public String getControllerName();

	public String getName();

	public Integer getPin();

	

	/**
	 * moveTo moves the servo to a specific location. Typically, a servo has 0
	 * to 180 positions
	 * 
	 * @param newPos
	 */
	public void moveTo(int newPos);

	/**
	 * Attach a servo controller to the servo. The servo and servo controller
	 * "should be in the same instance of MRL and this reference to another
	 * service should be ok.
	 * 
	 * The servo controller uses this method to pass a reference of itself to
	 * the servo, to be used directly.
	 */

	public boolean setController(ServoController controller);

	
	public boolean setController(String controller);

	/**
	 * limits input of servo - to prevent damage or problems if servos should
	 * not move thier full range
	 * 
	 * @param max
	 */
	public void setMinMax(int min, int max);

	/**
	 * memory of the controllers pin - so that it can be re-attached after a
	 * detach
	 * 
	 * @return
	 */
	public boolean setPin(int pin);
	
	/**
	 * fractional speed settings
	 * @param speed
	 */
	public void setSpeed(int speed);
	public void setSpeed(double speed);

	/**
	 * stops the servo if currently in motion servo must be moving at
	 * incremental speed for a stop to work (setSpeed < 1.0)
	 */
	public void stop();


}
