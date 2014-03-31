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

package org.myrobotlab.serial.gnu;

import gnu.io.CommDriver;
import gnu.io.CommPort;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.serial.gnu.BinaryCommPort.LineDriver;

public class BinaryDriver implements CommDriver {

	public final static Logger log = LoggerFactory.getLogger(BinaryDriver.class.getCanonicalName());
	private BinaryCommPort bcp;

	public BinaryDriver(LineDriver ld) {
		bcp = new BinaryCommPort();
		bcp.setLineDriver(ld);
	}

	// @Override - only in Java 1.6
	public CommPort getCommPort(String arg0, int arg1) {
		log.info("getCommPort");

		return bcp;
	}

	// @Override - only in Java 1.6
	public void initialize() {
		log.info("BinaryDriver.initialize");
	}

}