package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

public class CleverBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(CleverBot.class.getCanonicalName());

	transient ChatterBotFactory factory = null;
	transient ChatterBotSession session = null;
	transient ChatterBot chatterbot = null;
	transient ChatterBotType type = ChatterBotType.PANDORABOTS;
	boolean initialized = false;
	boolean continueToTalkToSelf = true;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {
			CleverBot cleverbot = new CleverBot("cleverbot");
			cleverbot.startService();
			log.info(cleverbot.chat("Hi"));

			log.info(cleverbot.chat("How are you?"));

			log.info("here");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public CleverBot(String n) {
		super(n);
		init();
	}

	public String chat(String toSay) {

		try {
			return session.think(toSay);
		} catch (Exception e) {
			Logging.logError(e);
		}

		return null;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "intellegence" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public boolean init() {
		try {
			factory = new ChatterBotFactory();
			// chatterbot = factory.create(type);
			chatterbot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
			// chatterbot = factory.create(ChatterBotType.CLEVERBOT);
			session = chatterbot.createSession();
		} catch (Exception e) {
			Logging.logError(e);
		}
		return true;
	}

	public String talkToSelf(String input) {
		try {

			ChatterBot bot1 = factory.create(ChatterBotType.CLEVERBOT);
			ChatterBotSession bot1session = bot1.createSession();

			ChatterBot bot2 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
			ChatterBotSession bot2session = bot2.createSession();

			while (continueToTalkToSelf) {

				System.out.println("bot1> " + input);

				input = bot2session.think(input);
				log.info(input);
				System.out.println("bot2> " + input);
				log.info(input);

				input = bot1session.think(input);
			}
		} catch (Exception e) {
			Logging.logError(e);
		}

		return input;
	}

}
