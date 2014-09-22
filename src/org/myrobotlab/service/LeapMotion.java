package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.Math;
import com.leapmotion.leap.*;
import com.leapmotion.leap.Gesture.State;

public class LeapMotion extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion.class);

	public LeapMotion(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "Service to control a LeapMotion Device";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			LeapMotion leap = (LeapMotion)Runtime.start("leap", "LeapMotion");
			leap.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
