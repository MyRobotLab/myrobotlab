package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Agent extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Agent.class);

	public Agent(String n) {
		super(n);
	}

	public Status test() {
		Status status = Status.info("agent test begin");

		try {
		//JUnitCore junit = new JUnitCore();
		// Result result = junit.run(testClasses);
		
		/*
		Bootstrap test = new BootstrapHotSpot();
		// spawn mrl instance
		Process process = test.spawn(new String[]{"-service", "test", "Test"});
		//Process process = test.spawn(new String[]{});
		 * 
		 */
		
		// process.destroy();
		} catch(Exception e){
			Logging.logException(e);
		}

		return status;
	}

	@Override
	public String getDescription() {
		return "the bootstrap agent";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Agent agent = (Agent) Runtime.start("agent", "Agent");
		//agent.test();
	}

}
