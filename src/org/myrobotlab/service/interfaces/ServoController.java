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

public interface ServoController extends DeviceController {

	void attach(ServoControl servo) throws Exception;
	
	void attach(ServoControl servo, int pin) throws Exception;
	
	// this is Arduino's servo.attach
	// void servoAttach(ServoControl servo, int pin, Integer targetOutput, Integer velocity);
	
	/**
	 * Arduino's servo.attach(pin) which is just energizing on a pin
	 */
	void servoAttachPin(ServoControl servo, int pin);

	void servoSweepStart(ServoControl servo);

	void servoSweepStop(ServoControl servo);

	void servoMoveTo(ServoControl servo);

	void servoWriteMicroseconds(ServoControl servo, int uS);

	void servoDetachPin(ServoControl servo);

	void servoSetMaxVelocity(ServoControl servo);

	void servoSetVelocity(ServoControl servo);

	void servoSetAcceleration(ServoControl servo);
	

}
