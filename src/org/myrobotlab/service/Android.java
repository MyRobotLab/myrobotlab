package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Android extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Android.class);
	
	public static class Motion {
		public double x;
		public double y;
		public double z;
		
		public Motion(double x, double y, double z){
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	// SO the Webgui Does NOT USE THE INBOX BUT INVOKES
	// DIRECTLY !!!! ON THE SERVICE !! ONE DOWNSIDE OF THIS IS
	// THE RESULT IS NOT PUT ON THE BUS !!! - PERHAPS IT SHOULD USE THE INBOX !!!!
	public void motion(double x, double y, double z)
	{
		log.info("x {} y {} z {}", x, y , z);
		
		invoke("publishMotion", new Motion(x, y, z));
		//return publishMotion();
	}
	
	public Motion publishMotion(Motion m){
		return m;
	}
	
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Android template = (Android) Runtime.start("template", "_TemplateService");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Android(String n) {
		super(n);
	}


	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
}
