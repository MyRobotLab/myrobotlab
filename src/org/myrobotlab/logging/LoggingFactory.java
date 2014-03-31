package org.myrobotlab.logging;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;

public class LoggingFactory {
	
	public static Logging getInstance()
	{
		try {
			Logging logging = (Logging)Service.getNewInstance(Platform.getVMName().equals(Platform.VM_DALVIK)?"org.myrobotlab.logging.LoggingLog4J":"org.myrobotlab.logging.LoggingLog4J");
			return logging;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			Logging.logException(e); // 
			//Logging.loge
			// TODO Auto-generated catch block
			// FIXME - log it
			e.printStackTrace();
		}
		
		return null;
	}

}
