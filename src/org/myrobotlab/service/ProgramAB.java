package org.myrobotlab.service;

import java.io.File;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;

/**
 * Program AB service for MyRobotLab
 * Uses AIML 2.0 to create a ChatBot
 * This is a reboot of the Old AIML spec to be more 21st century.
 * 
 * More Info at http://aitools.org/ProgramAB 
 * 
 * @author kwatters
 *
 */
public class ProgramAB extends Service {

	private Bot bot=null;
	private Chat chatSession=null;
	
	public ProgramAB(String reservedKey) {
		super(reservedKey);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public String getDescription() {
		return "AIML 2.0 Reference interpreter based on Program AB";
	}


	/**
	 * Load the AIML 2.0 Bot config and start a chat session.  This must be called after the service is created.
	 * 
	 * @param path - should be the full path to the ProgramAB root
	 * @param botName - The name of the bot to load. (example: alice2)
	 */
	public void startSession(String path, String botName) {
		// TODO don't allow to specify a different path
		// it will be assumed to be ./ProgramAB
		bot = new Bot(botName, path);
		chatSession = new Chat(bot);				
	}

	/**
	 * 
	 * @param text - the query string to the bot brain
	 * @param userId - the user that is sending the query
	 * @param robotName - the name of the bot you which to get the response from
	 * @return
	 */
	public String getResponse(String text) {
		if (bot == null || chatSession == null) {
			return "ERROR: Core not loaded, please load core before chatting.";
		}
		String res = chatSession.multisentenceRespond(text);
		System.out.println(res);
		return res;
	}
	
	

	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		ProgramAB alice = (ProgramAB) Runtime.createAndStart("alice", "ProgramAB");
		File f = new File("ProgramAB");
		String progABPath = f.getAbsolutePath();
		String botName = "alice2";
		alice.startSession(progABPath, botName); 
		String response = alice.getResponse("How are you?");
		System.out.println("Alice" + response);		
	}

}