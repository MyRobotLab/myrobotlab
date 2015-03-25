package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         References :
 *         http://www.codeproject.com/Articles/36106/Chatbot-Tutorial
 *         http://cleverbot.com/
 *         http://courses.ischool.berkeley.edu/i256/f06/projects/bonniejc.pdf
 *         http://www.infradrive.com/downloads/articles/Article1.pdf
 *         http://www.chatterbotcollection.com/
 * 
 */
public class ChatBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ChatBot.class.getCanonicalName());

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {

			ChatBot template = new ChatBot("chatbot");
			template.startService();
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public ChatBot(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "intellegence" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
