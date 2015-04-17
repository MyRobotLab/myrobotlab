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

import gnu.io.CommPortIdentifier;

import java.io.OutputStream;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Logging;

public class ParallelPort extends Service {

	private static final long serialVersionUID = 1L;

	private static OutputStream outputStream;;

	private static gnu.io.ParallelPort parallelPort;
	private static CommPortIdentifier port;

	public static final String[] PORT_TYPE = { "Serial Port", "Parallel Port" };

	public void write(int data) {
		try {
			outputStream.write(data);
		} catch (Exception e) {
			error(e);
		}
	}

	public boolean connect(String name) {
		try {
			port = CommPortIdentifier.getPortIdentifier(name);
			// if (port.getPortType()) - TODO - identify port return false if !=
			// parallel port
			parallelPort = (gnu.io.ParallelPort) port.open("CommTest", 50);
			outputStream = parallelPort.getOutputStream();
			return true;
		} catch (Exception e) {
			error(e);
		}
		return false;
	}
	
	public void disconnect(){
		try {
		parallelPort.close();
		} catch(Exception e){
			Logging.logError(e);
		}
	}

	public static void main(String[] args) {
		try {
			ParallelPort pp = (ParallelPort)Runtime.start("parallel", "ParallelPort");
			pp.connect("LPT1");
			pp.write(8);
			pp.disconnect();
		} catch(Exception e){
			Logging.logError(e);
		}
	
	}

	public ParallelPort(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "sensor", "control" };
	}

	@Override
	public String getDescription() {
		return "parallel port";
	}

}
