package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the chrome web browser
 * this service requires the webgui to be running.
 *
 */
public class WebkitSpeechRecognition extends Service implements SpeechRecognizer, TextPublisher {

	
	/**
	 * TODO: make it's own class.
	 * TODO: merge this data structure with the programab oob stuff?
	 *
	 */
	public class Command {
		public String name;
		public String method;
		public Object[] params;

		Command(String name, String method, Object[] params) {
			this.name = name;
			this.method = method;
			this.params = params;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String language = "en-US";
	
	HashMap<String, Command> commands = null;
	
	public WebkitSpeechRecognition(String reservedKey) {
		super(reservedKey);
	}

	@Override
	public String publishText(String text) {
		log.info("Publish Text : {}", text);
		// TODO: is there a better place to do this?  maybe recognized?
		if (commands.containsKey(text)) {
			// If we have a command. send it when we recognize...
			Command cmd = commands.get(text);
			send(cmd.name, cmd.method, cmd.params);
		}
		
		return text;
	}

	@Override
	public void listeningEvent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pauseListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public String recognized(String text) {
		log.info("Recognized : {}", text);
		if (commands.containsKey(text)) {
			// If we have a command. send it when we recognize...
			Command cmd = commands.get(text);
			send(cmd.name, cmd.method, cmd.params);
		}
		return text;
	}

	@Override
	public void resumeListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopListening() {
		// TODO Auto-generated method stub
	}
	
	public void setLanguage(String language) {
		// Here we want to set the language string and broadcast the update to the
		// web gui so that it knows to update the language on webkit speech
		this.language = language;
		broadcastState();
	}
	
	public String getLanguage() {
		// a getter for it .. just in case.
		return this.language;
	}

	@Override
	public void addTextListener(TextListener service) {
		addListener("publishText", service.getName(), "onText");
	}

	@Override
	public void addMouth(SpeechSynthesis mouth) {
		mouth.addEar(this);
		// TODO : we can implement the "did you say x?"
		// logic like sphinx if we want here.		
        // when we add the ear, we need to listen for request confirmation

	}

	@Override
	public void onStartSpeaking(String utterance) {
		// at this point we should subscribe to this in the webgui
		// so we can pause listening.
		
	}

	@Override
	public void onEndSpeaking(String utterance) {
		// need to subscribe to this in the webgui
		// so we can resume listening.
		
	}
	
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			Runtime.start("webgui", "WebGui");
			Runtime.start("webkitspeechrecognition", "WebkitSpeechRecognition");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(WebkitSpeechRecognition.class.getCanonicalName());
		meta.addDescription("Speech recognition using Google Chrome webkit");
		meta.addCategory("speech recognition");
		// meta.addPeer("tracker", "Tracking", "test tracking");
		return meta;		
	}

	@Override
	public void lockOutAllGrammarExcept(String lockPhrase) {
		log.warn("Lock out grammar not supported on webkit, yet...");
	}

	@Override
	public void clearLock() {
		log.warn("clear lock out grammar not supported on webkit, yet...");
	}
	
	
	// TODO - should this be in Service ?????
	public void addCommand(String actionPhrase, String name, String method, Object... params) {
		if (commands == null) {
			commands = new HashMap<String, Command>();
		}
		commands.put(actionPhrase, new Command(name, method, params));
	}
	
}
