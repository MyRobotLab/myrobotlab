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

public interface ServoControl extends DeviceControl, AbsolutePositionControl {

	// FIXME - do we want to support this & what do we expect from
	// 1. should it be energized when initially attached?
	// 2. should the position be set initially on attach ?
	// 3. should rest be set by pos if its not set already .. ie .. is the pos
	// passed in on attach the "rest" position of the servo ?
	// 4. should we 'please' rename servo.attach(pin) to servo.enablePwm(pin)
	// servo.disablePwm(pin)
	// !!!!

	/**
	 * The point of the 'attach' is a concept to the user of the Servo. A simple
	 * concept across all services where the "minimal" amount of
	 * complexity/parameters are needed to 'attach'
	 * 
	 * @param controller
	 * @param pin
	 * @throws Exception
	 */
	

	// preferred - sets control
	void attach(ServoController controller, Integer pin) throws Exception;
	void attach(ServoController controller, Integer pin, Integer pos) throws Exception;
	void attach(ServoController controller, Integer pin, Integer pos, Integer velocity) throws Exception;
	
	public boolean isAttached(ServoController controller);

	// should this be in Device Control - its not "ServoControl" specific
	void detach(ServoController controller);
	void detach(String controllerName);

	/**
	 * VERY DIFFERENT FROM SERVICE ATTACHEMENT !!!
	 * (an unfortunate use of name)
	 * Re-attaches (re-energizes) the servo on its current pin FIXME - should be
	 * renamed to energize
	 * 
	 * @return
	 */
	public void attach();
	
	/**
	 * degrees per second rotational velocity
	 * cm per second linear velocity ?
	 * @param velocity
	 */
	public void setVelocity(Integer velocity);

	/**
	 * Re-attaches (re-energizes) the servo on its current pin FIXME - should be
	 * renamed to energize(pin)
	 * 
	 * @return
	 */
	public void attach(int pin);

	/**
	 * calls Servo.detach() on MRLComm FIXME - should be renamed to de-energize
	 * (heh .. hyphons :P)
	 * 
	 * @return
	 */
	public void detach();

	/**
	 * limits input of servo - to prevent damage or problems if servos should
	 * not move thier full range
	 * 
	 * @param max
	 */
	public void setMinMax(int min, int max);

	/**
	 * fractional speed settings 0.0 to 1.0
	 * 
	 * @param speed
	 */
	public void setSpeed(double speed);

	/**
	 * stops the servo if currently in motion servo must be moving at
	 * incremental speed for a stop to work (setSpeed < 1.0)
	 */
	public void stop();

	/**
	 * configuration method - a method the controller will call when the servo
	 * is attached.
	 * 
	 * What should happen is if (controller != null) { pin =
	 * controller.servoGetPin(); } return pin; This returns the pin info the
	 * controller has - updates the Servo's pin and returns the refreshed data.
	 * Not worth it. What will happen is the pin which was set on the servo will
	 * simply be returned
	 * 
	 * @return
	 */
	public Integer getPin();

	/**
	 * a default position for the servo
	 * 
	 * @param rest
	 */
	public void setRest(int rest);

	/**
	 * command to move to the rest position
	 */
	public void rest();

	/**
	 * minimal sweep position sweep data need for the controller
	 * 
	 * @return
	 */
	public int getSweepMin();

	/**
	 * max sweep position sweep data need for the controller
	 * 
	 * @return
	 */
	public int getSweepMax();

	/**
	 * sweep step sweep data need for the controller
	 * 
	 * @return
	 */
	public int getSweepStep();

	/**
	 * the calculated output for the servo
	 */
	public Integer getTargetOutput();

	public double getSpeed();

	public int getMaxVelocity();

	int getVelocity();
	
	void setPin(int pin);

}
