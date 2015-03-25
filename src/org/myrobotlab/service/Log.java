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

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Log extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Log.class.getCanonicalName());

	/*
	 * TODO - allow options to record and playback message log - serialize to
	 * disk etc
	 */

	// TODO - do in Service
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			Log toy = new Log("logger");
			toy.startService();

			RemoteAdapter remote = new RemoteAdapter("remote");
			remote.startService();

			Runtime.createAndStart("rgui", "GUIService");

			/*
			 * GUIService gui = new GUIService("loggui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Log(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "testing" };
	}

	@Override
	public String getDescription() {
		return "logging service";
	}

	public Message log(Message m) {
		log.info("log message from " + m.sender + "." + m.data); // TODO -
																	// remove
																	// for debug
																	// only
		return m;
	}

	@Override
	public boolean preProcessHook(Message m) {
		if (m.method.equals("log")) {
			invoke("log", m);
			return false;
		}
		return true;
	}

}
