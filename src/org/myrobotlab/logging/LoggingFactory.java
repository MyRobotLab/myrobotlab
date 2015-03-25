package org.myrobotlab.logging;

import org.myrobotlab.framework.Service;

public class LoggingFactory {

	public static Logging getInstance() {
		try {
			Logging logging = (Logging) Service.getNewInstance("org.myrobotlab.logging.LoggingLog4J");
			return logging;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			Logging.logError(e); //
			// Logging.loge
			// TODO Auto-generated catch block
			// FIXME - log it
			e.printStackTrace();
		}

		return null;
	}

}
