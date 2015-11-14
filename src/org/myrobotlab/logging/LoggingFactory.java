package org.myrobotlab.logging;

import org.myrobotlab.framework.Service;

public class LoggingFactory {

	public static Logging getInstance() {
		try {
			//Logging logging = (Logging) Service.getNewInstance("org.myrobotlab.logging.LoggingLog4J");
			Logging logging = (Logging) Service.getNewInstance("org.myrobotlab.logging.LoggingSLF4J");
			return logging;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			Logging.logError(e); //
			e.printStackTrace();
		}

		return null;
	}

}
