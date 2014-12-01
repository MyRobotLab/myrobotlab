package org.myrobotlab.service;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.Chat;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.OOBPayload;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

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
public class ProgramAB extends Service implements TextListener,TextPublisher {

	private Bot bot = null;
	private String path = "ProgramAB";
	private String botName = "alice2";
	//private Chat chatSession=null;
	private HashMap<String, Chat> sessions = new HashMap<String, Chat>();
	
	// TODO: better parsing than a regex...
	private Pattern oobPattern = Pattern.compile("<oob>(.*?)</oob>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
	private boolean processOOB = true;

	private final Date serviceStartTime;
	// TODO: this should be per session, and probably not global
	private Date lastResponseTime = null;
	
	// Number of milliseconds before the robot starts talking on its own.
	private int maxConversationDelay = 5000;
	// boolean to turn on and off the auto conversation logic.
	private boolean enableAutoConversation = false;
	
	public ProgramAB(String reservedKey) {
		super(reservedKey);
		// we started.. 
		serviceStartTime = new Date();
		
	}

	private static final long serialVersionUID = 1L;

	@Override
	public String getDescription() {
		return "AIML 2.0 Reference interpreter based on Program AB";
	}

	public void reloadSession(String path, String botName) {
		reloadSession(path, null, botName);
	}

	public void reloadSession(String path, String session, String botName) {
		if (session == null){
			session = "default";
		}
		// kill the bot
		bot = null;
		// kill the session
		if (sessions.containsKey(session)) {
			// TODO: will garbage collection clean up the bot now ?
			// Or are there other handles to it?
			sessions.remove(session);
		}
		startSession(path, session, botName);
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

		// TODO : put some code here to delete the aimlif file 
		// if the aiml file is 

		cleanOutOfDateAimlIFFiles(botName, path);

		if (bot == null){
			bot = new Bot(botName, path);
		}
		if (log.isDebugEnabled()) {
			for (Category c : bot.brain.getCategories()) {
				log.debug(c.getPattern());
			}
		}
		sessions.put(session, new Chat(bot));

		if (!"default".equals(session)){
			getResponse(session, String.format("my name is %s", session));
		}

		// TODO: to make sure if the start session is updated, that the button updates in the gui
		// broadcastState();
	}

	private void cleanOutOfDateAimlIFFiles(String botName, String path) {
		String aimlPath = path + File.separator  + "bots" + File.separator + botName + File.separator + "aiml";
		String aimlIFPath = path + File.separator + "bots"+ File.separator + botName + File.separator + "aimlif";
		log.info("AIML FILES:");
		File folder = new File(aimlPath);
		System.out.println(folder.getAbsolutePath());
		HashMap<String, Long> modifiedDates = new HashMap<String, Long>();
		for (File f : folder.listFiles()) {
			log.info(f.getAbsolutePath());
			// TODO: better stripping of the file extension
			String aiml = f.getName().replace(".aiml", "");
			modifiedDates.put(aiml,f.lastModified());
		}
		log.info("AIMLIF FILES:");
		folder = new File(aimlIFPath);
		for (File f : folder.listFiles()) {
			log.info(f.getAbsolutePath());
			// TODO: better stripping of the file extension
			String aimlIF = f.getName().replace(".aiml.csv", "");
			Long lastMod = modifiedDates.get(aimlIF);
			if (lastMod != null) {
				if (f.lastModified() < lastMod) {
					// the AIMLIF file is newer than the AIML file.
					// delete the AIMLIF file so ProgramAB recompiles it properly.
					log.info("Deleteing AIMLIF file because the original AIML file was modified. {}", aimlIF);
					f.delete();
				}
			}
		}


	}

	public static class Response {
		public String session;
		public String msg;
		public OOBPayload payload;
		public Date timestamp;

		public Response(String session, String msg, OOBPayload payload, Date timestamp){
			this.session = session;
			this.msg = msg;
			this.payload = payload;
			this.timestamp = timestamp;
		}
	}

	/**
	 * Only respond if the last response was longer than delay ms ago
	 * 
	 * @param session - current session/username
	 * @param text - text to get a response for
	 * @param delay - min amount of time that must have transpired since the last response.
	 * @return
	 */
	public Response getResponse(String session, String text, long delay) {
		long delta = System.currentTimeMillis() - lastResponseTime.getTime();
		if (delta > delay) {
			return getResponse(session, text);
		} else {
			return null;
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
			return new Response(session, error, null, new Date());
		}

		System.out.println("BOT:" + bot.toString());

		if (!sessions.containsKey(session)){
			startSession(path, session, botName);
		}

		String res = sessions.get(session).multisentenceRespond(text);
		// grab and update the time when this response came in.
		lastResponseTime = new Date();
		
		// Check the AIML response to see if there is OOB (out of band data) 
		// If so, publish that data independent of the text response.
		OOBPayload payload = null;
		if (processOOB) {
			payload = processOOB(res);
		}

		// OOB text should not be published as part of the response text.
		Matcher matcher = oobPattern.matcher(res);
		res = matcher.replaceAll("");

		Response response = new Response(session, res, payload, lastResponseTime);
		// Now that we've said something, lets create a timer task to wait for N seconds 
		// and if nothing has been said.. try say something else.
		// TODO: trigger a task to respond with something again
		// if the humans get bored
		if (enableAutoConversation) {
			// TODO:  how do i properly pass params?
			Object[] params = new Object[]{session, text, maxConversationDelay};
			addLocalTask(maxConversationDelay, "getResponse", params);
		}
		
		// EEK! clean up the API!
		invoke("publishResponse", response);
		invoke("publishResponseText", response);
		invoke("publishText", response.msg);
		info("to: %s - %s", session, res);
		
		return response;
	}

	private OOBPayload processOOB(String text) {		
		Matcher oobMatcher = oobPattern.matcher(text);
		while (oobMatcher.find()) {
			// We found some OOB text.
			// assume only one OOB in the text?
			String oobPayload = oobMatcher.group(1);
			OOBPayload payload = parseOOB(oobPayload);
			// TODO: maybe we dont' want this? 
			// Notifiy endpoints
			invoke("publishOOBText", oobPayload);
			// grab service and invoke method.
			ServiceInterface s = Runtime.getService(payload.getServiceName());
			if (s == null) {
				log.warn("Service name in OOB tag unknown. {}" , oobPayload );
				return null;
			}
			if (payload.getParams()!=null) {
				s.invoke(payload.getMethodName(), payload.getParams().toArray());
			} else {
				s.invoke(payload.getMethodName());				
			}
			return payload;
		}
		return null;
	}

	private OOBPayload parseOOB(String oobPayload) {
		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(OOBPayload.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			log.info("OOB PAYLOAD :" + oobPayload);
			Reader r = new StringReader(oobPayload);
			OOBPayload oobMsg = (OOBPayload) jaxbUnmarshaller.unmarshal(r);
			return oobMsg;
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("OOB tag found, but it's not an MRL tag. {}", oobPayload );
		return null;
	}


	/** 
	 * Return a list of all patterns that the AIML Bot knows to match against.
	 * 
	 * @param botName
	 * @return
	 */
	public ArrayList<String> listPatterns(String botName) {
		ArrayList<String> patterns = new ArrayList<String>();
		for (Category c : bot.brain.getCategories()) {
			patterns.add(c.getPattern());
		}
		return patterns;
	}

	public void startSession(String progABPath, String botName) {
		startSession(progABPath	, null, botName);
	}

	/**
	 * Return the number of milliseconds since the last response was given
	 * -1 if a response has never been given.
	 * @return
	 */
	public long millisecondsSinceLastResponse() {
		if (lastResponseTime == null) {
			return -1;
		}
		long delta = System.currentTimeMillis()-lastResponseTime.getTime();
		return delta;
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

	/**
	 * If a response comes back that has an OOB Message, publish that separately
	 * 
	 * @param response
	 * @return
	 */
	public String publishOOBText(String oobText){
		return oobText;
	}

	public void addResponseListener(Service service){
		addListener("publishResponse", service.getName(), "onResponse", Response.class);
	}

	public void addTextListener(TextListener service){
		addListener("publishText", service.getName(), "onText", String.class);
	}

	public void addOOBTextListener(TextListener service){
		addListener("publishOOBText", service.getName(), "onOOBText", String.class);
	}

	public void addTextPublisher(TextPublisher service){
		addListener("publishText", service.getName(), "onText", String.class);
	}

	public void startSession() {
		startSession(null);
	}

	public void writeAIML() {
		bot.writeAIMLFiles();
	}

	public void writeAIMLIF() {
		bot.writeAIMLIFFiles();
	}

	public void writeAndQuit() {
		bot.writeQuit();
	}

	public void startSession(String session) {
		startSession(path, session, botName);
	}

	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		ProgramAB alice = (ProgramAB) Runtime.createAndStart("alice2", "ProgramAB");
		alice.startSession();
		Response response = alice.getResponse("Hello.");
		log.info("Alice " + response.msg);		
//		ProgramAB lloyd = (ProgramAB) Runtime.createAndStart("lloyd", "ProgramAB");
//		lloyd.startSession("ProgramAB", "default", "lloyd");
//		Response response = lloyd.getResponse("Hello.");
//		log.info("Lloyd " + response.msg);		
	}

	@Override
	public void onText(String text) {
		// What else should we do here?  seems reasonable to just do this.
		// this should actually call getResponse
		// on input, get the proper response
		Response resp = getResponse(text);
		// push that to the next end point.
		// invoke("publishText", resp.msg);
	}

	@Override
	public String publishText(String text) {
		return text;
	}

	public boolean isProcessOOB() {
		return processOOB;
	}

	public void setProcessOOB(boolean processOOB) {
		this.processOOB = processOOB;
	}

	public int getMaxConversationDelay() {
		return maxConversationDelay;
	}

	public void setMaxConversationDelay(int maxConversationDelay) {
		this.maxConversationDelay = maxConversationDelay;
	}

	public boolean isEnableAutoConversation() {
		return enableAutoConversation;
	}

	public void setEnableAutoConversation(boolean enableAutoConversation) {
		this.enableAutoConversation = enableAutoConversation;
	}



}