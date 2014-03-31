package org.myrobotlab.service;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;

public class Skype extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Skype.class.getCanonicalName());

	public Skype(String n) {
		super(n);
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Skype template = new Skype("template");
		template.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		
	}

}
