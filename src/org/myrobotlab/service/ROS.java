package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ROS extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ROS.class.getCanonicalName());

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		try {
			ROS ros = new ROS("ros");
			ros.startService();
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public ROS(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "bridge" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
