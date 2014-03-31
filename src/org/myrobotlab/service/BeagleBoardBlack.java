package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class BeagleBoardBlack extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(BeagleBoardBlack.class);
	
	public BeagleBoardBlack(String n) {
		super(n);	
	}
	
	@Override
	public String getDescription() {
		return "service to access the beagle board black hardware";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		BeagleBoardBlack bbb = new BeagleBoardBlack("bbb");
		bbb.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}


}
