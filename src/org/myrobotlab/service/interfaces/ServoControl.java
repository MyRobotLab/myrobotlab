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

	public String getName();

	/**
	 * Attach a servo controller to the servo. The servo and servo controller
	 * "should be in the same instance of MRL and this reference to another
	 * service should be ok.
	 * 
	 * The servo controller uses this method to pass a reference of itself to
	 * the servo, to be used directly.
	 */
	
	// FIXME - deprecate - part of attach !
	public boolean setController(ServoController controller);
	
	
	public boolean setController(String controller);
	
	
	/**
	 * The command accepts a parameter of offset which can be of the range -1.0
	 * to 1.0. Negative values are in one direction and positive values are in
	 * the opposite value. For example -1.0 would be maximum offset in a counter
	 * clock-wise direction and 0.9 would be 90% offset in a clockwise
	 * direction. 0.0 of course would be stop
	 * 
	 * @param offset
	 *            - new offset
	 */
	//public void move(Float offset);

	/**
	 * moveTo moves the servo to a specific location. Typically, a servo has 0
	 * to 180 positions
	 * 
	 * @param newPos
	 */
	public void moveTo(Integer newPos);

	/**
	 * gets the current position of the servo position is owned by the Servo
	 * 
	 * @return
	 */
	public Float getPosFloat();

	/**
	 * limits input of servo - to prevent damage or problems
	 * if servos should not move thier full range
	 * 
	 * @param max
	 */
	public void setMinMax(int min, int max);

	public String getControllerName();

	public Integer getPin();
	
	/**
	 * memory of the controllers pin - so that it can be re-attached
	 * after a detach
	 * @return
	 */
	public boolean setPin(int pin);
	
	public void setSpeed(Float speed);
	
	/**
	 * stops the servo if currently in motion
	 * servo must be moving at incremental speed for
	 * a stop to work (setSpeed < 1.0)
	 */
	public void stop();
	
	/**
	 * re-attaches servo to controller it was last
	 * attached to
	 * @return
	 */
	public boolean attach();	
	
	public boolean attach(String controller, Integer pin);
	
	/**
	 * detaches the servo from the controller
	 * @return
	 */
	public boolean detach();
	
	/**
	 * published event of a successful attach
	 * @return
	 */
	// public ServoControl attached(ServoControl sc);

	/**
	 * published detach event
	 * @return
	 */
	// public ServoControl detached(ServoControl sc);	
	
	/**
	 * published event when the pin is sent
	 * @param pin
	 * @return
	 */
	// public int pinSet(int pin);

}
