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

public interface ServoController {

	public final static String servoWrite = "servoWrite";
	public final static String servoAttach = "servoAttach";
	public final static String servoDetach = "servoDetach";

	public String getName();

	/**
	 * a list of pins from the controller which might be applicable for
	 * controlling a Servo
	 * 
	 * @return
	 */
	public ArrayList<Pin> getPinList();

	/**
	 * servoAttach - attach the servo to a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the servo
	 * @param pin
	 *            - pin number
	 * @return boolean boolean
	 */
	public boolean servoAttach(String servoName, Integer pin);

	/**
	 * servoDetach - detach the servo from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the servo
	 * @return boolean
	 */
	boolean servoDetach(String servoName);

	void servoSweepStart(String servoName, int min, int max, int step);

	public void servoSweepStop(String servoName);

	/**
	 * servoWrite - move the servo at an angle between 0 - 180
	 * 
	 * @param name
	 *            - name of the servo
	 * @param newPos
	 *            - positive or negative relative amount to move the servo
	 * @return void
	 */
	void servoWrite(String servoName, Integer newPos);

	public void servoWriteMicroseconds(String name, Integer ms);

	public boolean setServoEventsEnabled(String servoName, boolean b);

	/**
	 * return the current pin this servo is attached to
	 * 
	 * @param servoName
	 * @return
	 */
	// public Integer getServoPin(String servoName);

	public void setServoSpeed(String servoName, Float speed);

}
