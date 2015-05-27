/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exzception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * Dependencies:
 * sphinx4-1.0beta6
 * google recognition - a network connection is required
 * 
 * References:
 * Swapping Grammars - http://cmusphinx.sourceforge.net/wiki/sphinx4:swappinggrammars
 * 
 * http://cmusphinx.sourceforge.net/sphinx4/javadoc/edu/cmu/sphinx/jsgf/JSGFGrammar.html#loadJSGF(java.lang.String)
 * TODO - loadJSGF - The JSGF grammar specified by grammarName will be loaded from the base url (tossing out any previously loaded grammars)
 * 
 * 
 * */

package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.speech.DialogManager;
import org.slf4j.Logger;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

public class Sphinx extends Service implements SpeechRecognizer, TextPublisher {

	/**
	 * Commands must be created "before" startListening startListening will
	 * create a grammar file from the data
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

	class SpeechProcessor extends Thread {
		Sphinx myService = null;
		public boolean isRunning = false;

		public SpeechProcessor(Sphinx myService) {
			super(myService.getName() + "_ear");
			this.myService = myService;
		}

		@Override
		public void run() {

			try {
				isRunning = true;

				info(String.format("starting speech processor thread %s_ear", myService.getName()));

				String newPath = cfgDir + File.separator + myService.getName() + ".xml";
				File localGramFile = new File(newPath);

				info("loading grammar file");
				if (localGramFile.exists()) {
					info(String.format("grammar config %s", newPath));
					cm = new ConfigurationManager(newPath);
				} else {
					// resource in jar default
					info(String.format("grammar /resource/Sphinx/simple.xml"));
					cm = new ConfigurationManager(this.getClass().getResource("/resource/Sphinx/simple.xml"));
				}

				info("starting recognizer");
				// start the word recognizer
				recognizer = (Recognizer) cm.lookup("recognizer");
				recognizer.allocate();

				info("starting microphone");
				microphone = (Microphone) cm.lookup("microphone");
				if (!microphone.startRecording()) {
					log.error("Cannot start microphone.");
					recognizer.deallocate();
				}
				// loop the recognition until the program exits.
				isListening = true;
				while (isRunning) {

					info("listening");
					invoke("listeningEvent");

					Result result = recognizer.recognize();

					// log.error(result.getBestPronunciationResult());
					if (result != null) {
						String resultText = result.getBestFinalResultNoFiller();
						log.info("recognized: " + resultText + '\n');
						if (resultText.length() > 0 && isListening) {
							if (lockPhrases.size() > 0 && !lockPhrases.contains(resultText) && !confirmations.containsKey(resultText)) {
								log.info(String.format("but locked on %s", resultText));
								continue;
							}

							// command system being used
							if (commands != null) {

								if (currentCommand != null && (confirmations == null || confirmations.containsKey(resultText))) {
									// i have a command and a confirmation
									// command sent
									send(currentCommand.name, currentCommand.method, currentCommand.params);
									// command finished
									currentCommand = null;
									invoke("confirmed", "ok");

								} else if (currentCommand != null && negations.containsKey(resultText)) {
									// negation has happened... recognized the
									// wrong command
									// reset command
									currentCommand = null;
									// apologee
									invoke("negated", "sorry");
								} else if (commands.containsKey(resultText) && (confirmations != null || negations != null)) {
									if (bypass != null && bypass.containsKey(resultText)) {
										// we have confirmation and/or negations
										// - but we also have a bypass
										send(currentCommand.name, currentCommand.method, currentCommand.params);
									} else {
										// setting new potential command - using
										// either confirmations or negations
										Command cmd = commands.get(resultText);
										currentCommand = cmd;
										invoke("requestConfirmation", resultText);
									}
								} else if (commands.containsKey(resultText)) {
									// no confirmations or negations are being
									// used - just send command
									Command cmd = commands.get(resultText);
									send(cmd.name, cmd.method, cmd.params);
								} else {
									error(String.format("unknown use case for Sphinx commands - word is %s", resultText));
								}
							}

							// publishRecognized(resultText);
							invoke("publishText", resultText);
							invoke("recognized", resultText);
						}

					} else {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							logException(e);
						}
						// invoke("unrecognizedSpeech");
						log.error("I can't hear what you said.\n");
					}
				}
			} catch (Exception e) {
				error(e);
			}
		}

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sphinx.class.getCanonicalName());
	transient Microphone microphone = null;
	transient ConfigurationManager cm = null;
	transient Recognizer recognizer = null;

	transient DialogManager dialogManager = null;
	transient SpeechProcessor speechProcessor = null;

	private boolean isListening = false;
	// private String lockPhrase = null;
	HashSet<String> lockPhrases = new HashSet<String>();
	HashMap<String, Command> commands = null;
	HashMap<String, Command> confirmations = null;

	HashMap<String, Command> negations = null;

	HashMap<String, Command> bypass = null;

	Command currentCommand = null;

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			Sphinx ear = (Sphinx) Runtime.createAndStart("ear", "Sphinx");
			Speech speech = new Speech("speech");
			speech.startService();

			// attache speech to ear -
			// auto subscribes to "request confirmation"
			// so that speech asks for confirmation
			// TODO - put this in gui so state will be updated with text
			// question
			ear.attach(speech);

			Log log = (Log) Runtime.createAndStart("log", "Log");
			Clock clock = (Clock) Runtime.createAndStart("clock", "Clock");

			// TODO - got to do this - it will be KICKASS !
			// log.subscribe(outMethod, publisherName, inMethod, parameterType)
			// new MRLListener("pulse", log.getName(), "log");

			ear.addCommand("log", log.getName(), "log");
			ear.addCommand("log subscribe to clock", log.getName(), "subscribe", new Object[] { "pulse", });

			ear.addCommand("start clock", clock.getName(), "startClock");
			ear.addCommand("stop clock", clock.getName(), "stopClock");
			ear.addCommand("set clock interval to five seconds", clock.getName(), "setInterval", 5000);
			ear.addCommand("set clock interval to ten seconds", clock.getName(), "setInterval", 10000);

			ear.addComfirmations("yes", "correct", "right", "yeah", "ya");
			ear.addNegations("no", "incorrect", "wrong", "nope", "nah");

			ear.startListening();

			// ear.startListening("camera on | camera off | arm left | arm right | hand left | hand right ");
			// ear.startListening("yes | no");

			// Sphinx ear = new Sphinx("ear");
			// ear.createGrammar("hello | up | down | yes | no");
			// ear.startService();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Sphinx(String n) {
		super(n);
	}

	public void addBypass(String... txt) {
		if (bypass == null) {
			bypass = new HashMap<String, Command>();
		}
		Command bypassCommand = new Command(this.getName(), "bypass", null);

		for (int i = 0; i < txt.length; ++i) {
			bypass.put(txt[i], bypassCommand);
		}
	}

	public void addComfirmations(String... txt) {
		if (confirmations == null) {
			confirmations = new HashMap<String, Command>();
		}
		Command confirmCommand = new Command(this.getName(), "confirmation", null);

		for (int i = 0; i < txt.length; ++i) {
			confirmations.put(txt[i], confirmCommand);
		}
	}

	// TODO - should this be in Service ?????
	public void addCommand(String actionPhrase, String name, String method, Object... params) {
		if (commands == null) {
			commands = new HashMap<String, Command>();
		}
		commands.put(actionPhrase, new Command(name, method, params));
	}

	public void addNegations(String... txt) {
		if (negations == null) {
			negations = new HashMap<String, Command>();
		}
		Command negationCommand = new Command(this.getName(), "negation", null);

		for (int i = 0; i < txt.length; ++i) {
			negations.put(txt[i], negationCommand);
		}

	}

	public void addTextListener(TextListener service) {
		addListener("publishText", service.getName(), "onText", String.class);
	}

	public void addVoiceRecognitionListener(Service s) {
		// TODO - reflect on a public heard method - if doesn't exist error ?
		addListener("recognized", s.getName(), "heard", String.class);
	}

	// TODO - make "Speech" interface if desired
	public boolean attach(Speech mouth) {
		if (mouth == null) {
			warn("can not attach mouth is null");
			return false;
		}
		// if I'm speaking - I shouldn't be listening
		subscribe("isSpeaking", mouth.getName(), "isSpeaking", Boolean.class);
		// TODO - make config drive whether confirmation is desired
		mouth.subscribe("requestConfirmation", getName(), "requestConfirmation", String.class);
		mouth.subscribe("confirmed", getName(), "speak", String.class);
		mouth.subscribe("negated", getName(), "speak", String.class);
		log.info(String.format("attached Speech service %s to Sphinx service %s with default message routes", mouth.getName(), getName()));
		return true;
	}

	public void buildGrammar(StringBuffer sb, HashMap<String, Command> cmds) {
		if (cmds != null) {
			if (sb.length() > 0) {
				sb.append("|");
			}
			int cnt = 0;
			for (String key : cmds.keySet()) {
				++cnt;
				sb.append(key);
				if (cnt < cmds.size()) {
					sb.append("|");
				}
			}
		}
	}

	/*
	 * public void publishRecognized(String recognizedText) {
	 * invoke("recognized", recognizedText); }
	 */

	public void clearLock() {
		lockPhrases.clear();
	}

	public String confirmed(String txt) {
		return txt;
	}

	/**
	 * createGrammar must be called before the Service starts if a new grammar
	 * is needed
	 * 
	 * example: Sphinx.createGrammar ("ear", "stop | go | left | right | back");
	 * ear = Runtime.create("ear", "Sphinx")
	 * 
	 * @param filename
	 *            - name of the Service which will be utilizing this grammar
	 * @param grammar
	 *            - grammar content
	 * @return
	 */
	public boolean createGrammar(String grammar) {
		log.info("creating grammar [{}]", grammar);
		// FIXME - probably broken
		// get base simple.xml file - and modify it to
		// point to the correct .gram file
		String simplexml = getServiceResourceFile("simple.xml");
		// String grammarLocation = "file://" + cfgDir.replaceAll("\\\\", "/") +
		// "/";
		// simplexml = simplexml.replaceAll("resource:/resource/",
		// cfgDir.replaceAll("\\\\", "/"));
		simplexml = simplexml.replaceAll("resource:/resource/", ".myrobotlab");

		// a filename like i01.ear.gram (without the gram extention of course
		// because is sucks this out of the xml"
		// and re-processes it to be as fragile as possible :P
		String grammarFileName = getName();
		grammarFileName = grammarFileName.replaceAll("\\.", "_");
		if (grammarFileName.contains(".")) {
			grammarFileName = grammarFileName.substring(0, grammarFileName.indexOf("."));
		}

		simplexml = simplexml.replaceAll("name=\"grammarName\" value=\"simple\"", "name=\"grammarName\" value=\"" + grammarFileName + "\"");
		try {
			FileIO.stringToFile(String.format("%s%s%s.%s", cfgDir, File.separator, grammarFileName, "xml"), simplexml);
			save("xml", simplexml);

			String gramdef = "#JSGF V1.0;\n" + "grammar " + grammarFileName + ";\n" + "public <greet> = (" + grammar + ");";
			FileIO.stringToFile(String.format("%s%s%s.%s", cfgDir, File.separator, grammarFileName, "gram"), gramdef);
		} catch (Exception e) {
			Logging.logError(e);
			return false;
		}
		// save("gram", gramdef);

		return true;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "speech recognition", "control" };
	}

	@Override
	public String getDescription() {
		return "<html>speech recoginition service wrapping Sphinx 4</html>";
	}

	public boolean isRecording() {
		return microphone.isRecording();
	}

	/**
	 * an inbound port for Speaking Services (TTS) - which suppress listening
	 * such that a system will not listen when its talking, otherwise a feedback
	 * loop can occur
	 * 
	 * @param b
	 * @return
	 */
	public synchronized boolean isSpeaking(Boolean talking) {
		if (talking) {
			isListening = false;
			log.info("I'm talking so I'm not listening"); // Gawd, ain't that
															// the truth !
		} else {
			isListening = true;
			log.info("I'm not talking so I'm listening"); // mebbe
		}
		return talking;
	}

	/**
	 * Event is sent when the listening Service is actually listening. There is
	 * some delay when it initially loads.
	 */
	@Override
	public void listeningEvent() {
		return;
	}

	/**
	 * FIXME - the trunk is broke - the configuration is horrible find a way to
	 * make this work, despite Sphinx's chaos !
	 * 
	 * function to swap grammars to allow sphinx a little more capability
	 * regarding "new words"
	 * 
	 * check http://cmusphinx.sourceforge.net/wiki/sphinx4:swappinggrammars
	 * 
	 * @param newGrammarName
	 * @throws PropertyException
	 * @throws InstantiationException
	 * @throws IOException
	 */
	/*
	 * FIXME SPHINX IS A MESS IT CAN"T DO THIS ALTHOUGH DOCUMENTATION SAYS IT
	 * CAN void swapGrammar(String newGrammarName) throws PropertyException,
	 * InstantiationException, IOException { log.debug("Swapping to grammar " +
	 * newGrammarName); Linguist linguist = (Linguist)
	 * cm.lookup("flatLinguist"); linguist.deallocate(); // TODO - bundle
	 * sphinx4-1.0beta6 // cm.setProperty("jsgfGrammar", "grammarName",
	 * newGrammarName);
	 * 
	 * linguist.allocate(); }
	 */

	public void lockOutAllGrammarExcept(String lockPhrase) {
		this.lockPhrases.add(lockPhrase);
	}

	/*
	 * deprecated public void onCommand(String command, String targetName,
	 * String targetMethod, Object... data) { Message msg = new Message();
	 * msg.name = targetName; msg.method = targetMethod; msg.data = data;
	 * 
	 * commandMap.put(command, msg); }
	 */

	public String negated(String txt) {
		return txt;
	}

	/**
	 * method to suppress recognition listening events This is important when
	 * Sphinx is listening --> then Speaking, typically you don't want Sphinx to
	 * listen to its own speech, it causes a feedback loop and with Sphinx not
	 * really very accurate, it leads to weirdness -- additionally it does not
	 * recreate the speech processor - so its not as heavy handed
	 */
	@Override
	public void pauseListening() {
		isListening = false;
	}

	@Override
	public String publishText(String recognizedText) {
		return recognizedText;
	}

	/**
	 * The main output for this service.
	 * 
	 * @param word
	 * @return the word
	 */
	@Override
	public String recognized(String word) {
		return word;
	}

	public String requestConfirmation(String txt) {
		return txt;
	}

	@Override
	public void resumeListening() {
		isListening = true;
	}

	// FYI - grammar must be created BEFORE we start to listen
	@Override
	public void startListening() {
		startListening(null); // use existing grammar
	}

	// FIXME - re-entrant - make it create new speechProcessor
	// assume its a new grammar
	public void startListening(String grammar) {
		if (speechProcessor != null) {
			log.warn("already listening");
			return;
		}

		StringBuffer newGrammar = new StringBuffer();
		buildGrammar(newGrammar, commands);
		buildGrammar(newGrammar, confirmations);
		buildGrammar(newGrammar, negations);
		buildGrammar(newGrammar, bypass);

		if (grammar != null) {
			if (newGrammar.length() > 0) {
				newGrammar.append("|");
			}
			newGrammar.append(grammar);
		}

		createGrammar(newGrammar.toString());

		speechProcessor = new SpeechProcessor(this);
		speechProcessor.start();
	}

	@Override
	public void startRecording() {
		microphone.clear();
		microphone.startRecording();
	}

	@Override
	public void stopListening() {
		isListening = false;
		if (speechProcessor != null) {
			speechProcessor.isRunning = false;
		}
		speechProcessor = null;
	}

	/**
	 * stopRecording - it does "work", however, the speech recognition part
	 * seems to degrade when startRecording is called. I have worked around this
	 * by not stopping the recording, but by not processing what was recognized
	 */
	@Override
	public void stopMsgRecording() {
		microphone.stopRecording();
		microphone.clear();
	}

	@Override
	public void stopService() {
		super.stopService();
		stopListening();
		if (recognizer != null) {
			recognizer.deallocate();
			recognizer = null;
		}
		if (microphone != null) {
			microphone.stopRecording();
			microphone = null;
		}
	}
}
