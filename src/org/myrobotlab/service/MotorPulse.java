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
 * Pulse motor takes a series of digital pulses similar to pwm, however
 * it actually counts the position based on those pulses.  So its similar to 
 * a simple stepper.  MasterBlaster has this type of motor, I do not know how
 * direction control is managed
 * 
 */
public class MotorPulse extends Motor {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MotorPulse.class);
	Integer pulsePin;

	public MotorPulse(String n) {
		super(n);
	}
	
	public void setPulsePin(int pulsePin) {
		this.pulsePin = pulsePin;
	}
	
	public Integer getPulsePin() {
		return pulsePin;
	}
	
	@Override
	public void attach(MotorController controller) throws Exception {
		this.controller = controller;
		// FIXME controller.motorPulseAttach(pulsePin);
		// controller.deviceAttach(this, pulsePin);
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

		ServiceType meta = new ServiceType(MotorPulse.class.getCanonicalName());
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
}
