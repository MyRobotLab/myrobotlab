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

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

@Root
/* Finite State Machine Service */
public class FSM extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(FSM.class.getCanonicalName());

	HashMap<String, EventData> transistionStates = new HashMap<String, EventData>();

	// TODO - subsumption
	public class EventData {
		String name;
		String method;
		Object[] data;
		HashMap<String, Boolean> supressedStates;
	}

	public FSM(String n) {
		super(n);
	}

	public String inState(String newState) {
		if (transistionStates.containsKey(newState)) {
			EventData ed = transistionStates.get(newState);
			send(ed.name, ed.method, ed.data);
		}
		return newState;
	}



	@Override
	public String getDescription() {
		return "used to generate pulses";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		FSM fsm = new FSM("fsm");
		fsm.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		
	}

}
