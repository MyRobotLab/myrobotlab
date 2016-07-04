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

package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         MotorSimpleH represents a common continuous direct current electric motor controlled
 *         by a direction pin and a power pwm pin.  The direction pin is set high for one direction
 *         and low for the other.  The power pin determines speed.  Stopping the motor is
 *         simply stopping pwm on the power pin.  Max speed is when pwm pin is solid 1.
 * 
 */
public class MotorSimpleH extends Motor {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MotorSimpleH.class);
	Integer pwrPin;
	Integer dirPin;

	public MotorSimpleH(String n) {
		super(n);
	}
	
	public void setPwmPins(int pwrPin, int dirPin) {
		this.pwrPin = pwrPin;
		this.dirPin = dirPin;
	}
	
	public Integer getPwrPin() {
		return pwrPin;
	}
	
	public Integer getDirPin() {
		return dirPin;
	}


	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(MotorSimpleH.class.getCanonicalName());
		meta.addDescription("Motor service with 2 pwm pins");
		meta.addCategory("motor");

		return meta;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

		} catch (Exception e) {
			Logging.logError(e);
		}
	}
	
	@Override
	public void attach(MotorController controller) throws Exception {
		this.controller = controller;
		controller.deviceAttach(this, pwrPin, dirPin);
	}

}
