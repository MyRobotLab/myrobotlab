package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class OculusDIY extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OculusDIY.class);

	public OculusDIY(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public void onCustomMsg(Object[] data){
		System.out.println("yay");
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			OculusDIY oculus = (OculusDIY)Runtime.start("oculus", "OculusDIY");
			oculus.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
