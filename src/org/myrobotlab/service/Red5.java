package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Red5 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Red5.class.getCanonicalName());

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

	public Red5(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "video" };
	}

	@Override
	public String getDescription() {
		return "Red5 video/audio streaming service";
	}

}
