package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;


public class Hadoop extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Hadoop.class.getCanonicalName());
	
	public Hadoop(String n) {
		super(n);	
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Hadoop hadoop = new Hadoop("hadoop");
		hadoop.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}


}
