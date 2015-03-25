package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * FIXME - Implement http://magabot.cc/ :)
 * 
 * @author GroG
 *
 */
public class MagaBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MagaBot.class);

	public MagaBot(String n) {
		super(n);
	}

	/*
	 * xicombd: - '1' to Assisted Navigation - 'w' to go forward - 's' to go
	 * backward - 'a' to go left - 'd' to go right - 'p' to stop - '2' to
	 * Obstacle Avoidance - '3' to start Line Following
	 * 
	 * 'i' if the ir sensors are activated
	 */
	/*
	 * public void sendOrder(String o) { try { serialDevice.write(o); } catch
	 * (IOException e) { logException(e); } }
	 */

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
