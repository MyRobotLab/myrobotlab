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
 *         MotorDualPwm represents a common continuous direct current electric motor controlled
 *         by two Pwm pins.  Both control speed but in different directions.  When both pins
 *         are inactive the motor stops. When both pins are active the action is undefined,
 *         potentially it might make blue smoke - that would depend on the details of the 
 *         hardware configuration.
 * 
 */
public class MotorDualPwm extends Motor {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MotorDualPwm.class);
	Integer leftPin;
	Integer rightPin;

	public MotorDualPwm(String n) {
		super(n);
	}
	
	public void setPwmPins(int leftPin, int rightPin) {
		this.leftPin = leftPin;
		this.rightPin = rightPin;
	}
	
	public Integer getLeftPin() {
		return leftPin;
	}
	
	public Integer getRightPin() {
		return rightPin;
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

		ServiceType meta = new ServiceType(MotorDualPwm.class.getCanonicalName());
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
		controller.deviceAttach(this, leftPin, rightPin);
	}
	
}
