package org.myrobotlab.service;

import java.io.File;
import java.util.HashMap;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;

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

	private Bot bot = null;
	private String path = "ProgramAB";
	private String botName = "alice2";
	//private Chat chatSession=null;
	private HashMap<String, Chat> sessions = new HashMap<String, Chat>();
	
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
	public void startSession(String path, String session, String botName) {
		if (session == null){
			session = "default";
		}
		
		if (sessions.containsKey(session)){
			warn("session %s already created", session);
			return;
		}
		// TODO don't allow to specify a different path
		// it will be assumed to be ./ProgramAB
		if (bot == null){
			bot = new Bot(botName, path);
		}
		
		sessions.put(session, new Chat(bot));
		
		if (!"default".equals(session)){
			getResponse(session, String.format("my name is %s", session));
		}
	}
	
	public static class Response {
		public String session;
		public String msg;
		
		public Response(String session, String msg){
			this.session = session;
			this.msg = msg;
		}
	}

	public Response getResponse(String text){
		return getResponse(null, text);
	}
	
	/**
	 * 
	 * @param text - the query string to the bot brain
	 * @param userId - the user that is sending the query
	 * @param robotName - the name of the bot you which to get the response from
	 * @return
	 */
	public Response getResponse(String session, String text) {
		if (session == null){
			session = "default";
		}
		if (bot == null) {
			String error = "ERROR: Core not loaded, please load core before chatting.";
			error(error);
			return new Response(session, error);
		}
		
		if (!sessions.containsKey(session)){
			startSession(path, session, botName);
		}
		
		String res = sessions.get(session).multisentenceRespond(text);
		Response response = new Response(session, res);
		invoke("publishResponse", response);
		invoke("publishResponseText", response);
		info("to: %s - %s", session, res);
		return response;
	}
	

	public void startSession(String progABPath, String botName) {
		startSession(progABPath	, null, botName);
	}

	
	/**
	 * publishing method of the pub sub pair - with addResponseListener allowing subscriptions
	 * pub/sub routines have the following pattern
	 * 
	 * publishing routine -> publishX - must be invoked to provide data to subscribers
	 * subscription routine -> addXListener - simply adds a Service listener to the notify framework
	 * any service which subscribes must implement -> onX(data) - this is where the data will be sent (the call-back)
	 * 
	 * @param response
	 * @return
	 */
	public Response publishResponse(Response response){
		return response;
	}
	
	/**
	 * Test only publishing point - for simple consumers
	 * @param response
	 * @return
	 */
	public String publishResponseText(Response response){
		return response.msg;
	}

	public void addResponseListener(Service service){
		addListener("publishResponse", service.getName(), "onResponse", Response.class);
	}
	
	public void addTextListener(TextListener service){
		addListener("publishResponseText", service.getName(), "onText", String.class);
	}
	
	public void startSession() {
		startSession(null);
	}

	public void startSession(String session) {
		startSession(path, session, botName);
	}

	
	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		ProgramAB alice = (ProgramAB) Runtime.createAndStart("alice", "ProgramAB");
		// File f = new File("ProgramAB");
		// String progABPath = f.getAbsolutePath();
		// String botName = "alice2";
		
		alice.startSession(); 
		Response response = alice.getResponse("How are you?");
		
		alice.startSession("GroG"); 
		response = alice.getResponse("GroG", "How are you?");
		log.info("Alice " + response.msg);		
	}


}