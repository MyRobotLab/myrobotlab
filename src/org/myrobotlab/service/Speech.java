/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
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
 * */

package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Speech extends Service implements TextListener {

	/*
	 * Speech supports 2 different text to speech systems One is FreeTTS and the
	 * other is a remote/cloud access of ATTs online implementation. The FreeTTS
	 * is a complete voice system and can be loaded with different
	 * external/thirdParty voices.
	 * 
	 * The ATT is probably on the edge of licensing. An online system at ATT is
	 * available to use. This service will send the text to that online service,
	 * download the file and play it. Once it is downloaded, it will use the
	 * same file each time the text phrase is requested.
	 * 
	 * There is a front-end set of functions and a back-end set of functions.
	 * The front-end concerns how the calling process and request will be
	 * handled. There are 4 types of speaking. speakNormal - when this function
	 * is utilized, it means any simultaneous requests for speech will be
	 * dropped. This most closely approximates human speech. You may have a
	 * bazillion thoughts going on in your head but you only have 1 mouth.
	 * speakQueued - this function queues up all of the requests for speech and
	 * will speak each one until done. This can have the behavior of being very
	 * out of context, as speaking takes considerable time relative to many
	 * other processes. speakBlocking - This blocks the calling thread until the
	 * speak function is finished. I can see very little meaningful use for
	 * this. speakMulti - This will create threads for each requests possibly
	 * allowing every speech thread to complete in the same time. (very Cybil)
	 * The back-end are just types of speech engines (ATT, FREETTS)
	 * 
	 * References : Excellent reference -
	 * http://www.codeproject.com/Articles/435434/Text-to-Speech-tts-for-the-Web
	 * http://www.text2speech.org/ - another possible back-end
	 */

	// FIXME - Speech doesn't need HTTPClient - could just use
	// org.myrobotlab.net.HTTPRequest - and benefit from
	// 1 less dependency & proxy info

	public static enum BackendType {
		ATT, FREETTS, GOOGLE
	}

	public static enum FrontendType {
		NORMAL, QUEUED, BLOCKING, MULTI
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Speech.class.getCanonicalName());

	// en_us en_gb en_au en_sa en_nz
	// String language = "en";
	// By using google's ".co.uk" translate site's GET request, you can generate
	// British English.
	// http://translate.google.co.uk/translate_tts?q=Your+soundcard+works+perfectly&tl=en
	// routing
	// http://translate.google.com.mx/translate_tts?tl=en&q=hello+this+is+google
	// http://translate.google.com/translate_tts?tl=zh_CN&q=%E4%BD%A0%E5%A5%BD%E3%80%82%E6%82%A8%E4%B9%9F%E5%8F%AF%E4%BB%A5%E8%AE%B2%E4%B8%AD%E6%96%87%E3%80%82
	String language = "en_gb";

	// TODO - seperate all of the var into appropriate parts - ie Global ATT
	// Google FreeTTS

	private String googleURI = "http://translate.google.com/translate_tts?tl=%s&q=";
	static String filter = "[\\\\/:\\*\\?\"<>\\|]";
	transient private Voice myVoice = null;
	private boolean initialized = false;

	public AudioFile audioFile = null;;

	transient private DefaultHttpClient client = new DefaultHttpClient();;

	public String voiceName = "audrey"; // both voice systems have a list of
	// available voice names

	public FrontendType frontendType = FrontendType.NORMAL;
	public BackendType backendType = BackendType.GOOGLE;

	boolean fileCacheInitialized = false;

	private boolean isSpeaking = false;
	private String isSaying;

	final public static HashMap<String, String> googleLanguageMap = new HashMap<String, String>();

	public String googleProxyHost = null;
	public int googleProxyPort = 8080;
	public int afterSpeechPause = 600;

	public final static String BACKEND_TYPE_ATT = "ATT";

	public final static String BACKEND_TYPE_FREETTS = "FREETTS";

	public final static String BACKEND_TYPE_GOOGLE = "GOOGLE";

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("audioFile", "AudioFile", "plays tts files");
		return peers;
	}
	// codes - http://code.google.com/apis/language/translate/v2/using_rest.html
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			Speech mouth = (Speech) Runtime.start("mouth", "Speech");
			//mouth.setVolume(0.1F);
			
			mouth.test();

			String test = " hello this is a test \\dev\\blah / blah : * ? \" blah \" blah > < <> bla | zod | zod2 ".replaceAll(filter, " ");

			log.info(test);
			Speech speech = new Speech("speech");
			speech.startService();

			speech.speakFreeTTS("hello");

			speech.setBackendType(BACKEND_TYPE_FREETTS);

			speech.speak("blah blah system check completed sir");
			speech.speak("dood this is awesome");

			speech.setGenderMale();

			// speech.setBackendType(BACKEND_TYPE_GOOGLE);
			// speech.setLanguage("fr");
			speech.speakBlocking("this should work");
			speech.speakBlocking("bork bork bork bork again more more");
			speech.speak("did you say start clock");
			speech.speak("hello it is a pleasure to meet you I am speaking.  I do love to speak. What should we talk about. I love to talk I love to talk");
			speech.speak("goodby this is an attempt to generate inflection did it work");
			speech.speak("blah there. this is a long and detailed message");
			speech.speak("1 2 3 4 5 6 7 8 9 10, i know how to count");
			speech.speak("the time is 12:30");
			speech.speak("oink oink att is good but not so good");
			speech.speak("num, num, num, num, num");
			speech.speak("charging");
			speech.speak("thank you");
			speech.speak("good bye");
			speech.speak("I believe I have bumped into something");
			speech.speak("Ah, I have found a way out of this situation");
			speech.speak("aaaaaaaaah, long vowels sound");

			speech.setGoogleURI("http://tts-api.com/tts.mp3?q=");
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Speech(String n) {
		super(n);
		log.info("Using voice: " + voiceName);

		googleLanguageMap.put("english", "en");
		googleLanguageMap.put("danish", "da");
		googleLanguageMap.put("dutch", "nl");
		googleLanguageMap.put("german", "de");
		googleLanguageMap.put("french", "fr");
		googleLanguageMap.put("japanese", "ja");
		googleLanguageMap.put("portuguese", "pt");

		audioFile = (AudioFile) createPeer("audioFile");
	}

	private String cleanFilename(String toSpeak) {
		// Strip all chars that are not valid to use in a filename
		// windows list
		// A filename cannot contain any of the following characters:
		// \ / : * ? " < > |
		// ; is forbidden on unix.. not sure what else we need
		// get rid of parens also maybe?
		// TODO: find a nice clean list / library to do this
		String cleanSpeak = toSpeak.replaceAll("^[.\\\\/:;*?\"<>|]?[\\\\/:*?\"<>|\\(\\)]*", " ");
		// cr/lf are not good in file names.
		cleanSpeak = cleanSpeak.replaceAll("\r", " ");
		cleanSpeak = cleanSpeak.replaceAll("\n", " ");
		cleanSpeak = cleanSpeak.replaceAll("  ", " ");
		return cleanSpeak.trim().toLowerCase();
	}

	public byte[] getByteArrayFromResponse(HttpResponse response) {
		try {
			InputStream is = response.getEntity().getContent();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();

			return buffer.toByteArray();
		} catch (Exception e) {
			Logging.logError(e);
		}

		return null;

	}

	@Override
	public String[] getCategories() {
		return new String[] { "speech", "sound" };
	}

	@Override
	public String getDescription() {
		return "speech synthesis service";
	}

	public synchronized Boolean isSpeaking(Boolean b) {
		log.info("isSpeaking " + b);
		isSpeaking = b;
		return isSpeaking;
	}

	// get list of voices from back-end
	public ArrayList<String> listAllVoices() {
		log.info("All voices available:");
		ArrayList<String> voiceList = new ArrayList<String>();
		if (backendType == BackendType.FREETTS) {
			VoiceManager voiceManager = VoiceManager.getInstance();
			Voice[] voices = voiceManager.getVoices();
			for (int i = 0; i < voices.length; i++) {
				log.info("    " + voices[i].getName() + " (" + voices[i].getDomain() + " domain)");
				voiceList.add(voices[i].getName());
			}
		} else if (backendType == BackendType.ATT) {
			// TODO get list of att voices
			// could do it dynamically... not yet
			voiceList.add("crystal");
			voiceList.add("mike");
			voiceList.add("rich");
			voiceList.add("lauren");

			voiceList.add("audrey");

		} else {
			log.error("voice backendType " + backendType + " not supported");
		}

		return voiceList;
	}

	@Override
	public void onText(String text) {
		speak(text);
	}

	// FIXME - WTF ARE YOU DOING THIS ????
	public void queueSetLanguage(String l) {
		fileCacheInitialized = false;
		language = l;
	}

	/**
	 * request confirmation of recognized text this typically comes from a
	 * speech recognition service and is a verbal query - asking if they heard
	 * the correct phrase
	 * 
	 * @param text
	 */
	public void requestConfirmation(String text) {
		speak(String.format("did you say. %s", text));
	}

	public synchronized String saying(String t) {
		isSaying = t;
		return isSaying;
	}

	public void setBackendType(String t) {
		if (BACKEND_TYPE_ATT.equals(t)) {
			backendType = BackendType.ATT;
		} else if (BACKEND_TYPE_FREETTS.equals(t)) {
			backendType = BackendType.FREETTS;
		} else if (BACKEND_TYPE_GOOGLE.equals(t)) {
			backendType = BackendType.GOOGLE;
		} else {
			log.error("type " + t + " not supported");
		}
	}

	public void setFrontendType(String t) {
		if ("NORMAL".equals(t)) {
			frontendType = FrontendType.NORMAL;
		} else if ("QUEUED".equals(t)) {
			frontendType = FrontendType.QUEUED;
		} else if ("BLOCKING".equals(t)) {
			frontendType = FrontendType.BLOCKING;
		} else if ("MULTI".equals(t)) {
			frontendType = FrontendType.MULTI;
		} else {
			log.error("type " + t + " not supported");
		}
	}

	public void setGenderFemale() {
		voiceName = "audrey";
		googleProxyHost = null;
	}

	public void setGenderMale() {
		voiceName = "jarvis";

		googleProxyHost = "94.23.0.183";
		googleProxyPort = 80;

		googleProxyHost = "91.236.255.195";
		googleProxyPort = 3128;

		googleProxyHost = "94.23.29.189";
		googleProxyPort = 8080;

		googleProxyHost = "91.121.11.120";
		googleProxyPort = 8080;

	}

	public void setGoogleProxy(String voiceName, String host, int port) {
		this.voiceName = voiceName;
		this.googleProxyHost = host;
		this.googleProxyPort = port;
	}

	public void setGoogleURI(String uri) {
		googleURI = uri;
	}

	public void setLanguage(String l) {
		in(createMessage(getName(), "queueSetLanguage", l));
	}

	// front-end functions
	
	public boolean speak(String toSpeak) {
		//  TODO: smart chunk the speech ..
		
		System.err.println("to Speak " + toSpeak);
		boolean result = false;
		boolean remainingText = true;
		String buff = toSpeak;
		
		while (remainingText) {
			// find the first chunk
			int maxUtterance = 100;
			if (buff.length() < 100) {
				//System.err.println(buff);
				result = speakInternal(buff);
				break;
			}
			
			int lastSpace = buff.substring(0, maxUtterance).lastIndexOf(" ");
			if (lastSpace == -1 ) {
				// that's it. 
				//System.err.println(buff);
				result = speakInternal(buff);
				break;
			}
			String currBuff = buff.substring(0, lastSpace);
			//System.err.println(currBuff);
			result = speakInternal(currBuff);
			if (!result) {
				break;
			}
			buff = buff.substring(lastSpace);
		}
		return result;
	}
	
	public boolean speakInternal(String toSpeak) {
		toSpeak = toSpeak.replaceAll(filter, " ");
		if (toSpeak == null || toSpeak.length() == 0) {
			return false;
		}
		if (frontendType == FrontendType.NORMAL) {
			return speakNormal(toSpeak);
		} else if (frontendType == FrontendType.QUEUED) {
			// speakQueued
		}
		return false;
	}

	public boolean speakBlocking(String toSpeak) {
		return speakBlocking(toSpeak, (Object[]) null);
	}

	/**
	 * main speak blocking function
	 * 
	 * @param speak
	 * @param fdata
	 * @return
	 */
	public boolean speakBlocking(String speak, Object... fdata) {
		if (speak == null || speak.length() == 0) {
			return false;
		}

		String toSpeak = String.format(speak, fdata).replaceAll(filter, " ");

		if (backendType == BackendType.FREETTS) { // festival tts
			speakFreeTTS(toSpeak);
		} else if (backendType == BackendType.GOOGLE) { // google tts
			speakGoogle(toSpeak);
		} else {
			log.error("back-end speech backendType " + backendType + " not supported ");
			return false;
		}
		return true;
	}

	public void speakErrors(boolean b) {
		// register for Runtime registered (new services)

		// get all current services
		// register for their errors
		List<ServiceInterface> services = Runtime.getServices();
		if (b) {
			for (int i = 0; i < services.size(); ++i) {
				ServiceInterface sw = services.get(i);
				subscribe(sw.getName(), "publishError", "speak");
				// this.addListener(outMethod, namedInstance, inMethod);
			}
		} else {
			for (int i = 0; i < services.size(); ++i) {
				ServiceInterface sw = services.get(i);
				unsubscribe(sw.getName(), "publishError", "speak");
				// this.addListener(outMethod, namedInstance, inMethod);
			}
		}
	}

	public void speakFreeTTS(String toSpeak) {
		if (myVoice == null) {
			// The VoiceManager manages all the voices for FreeTTS.
			VoiceManager voiceManager = VoiceManager.getInstance();
			Voice[] possibleVoices = voiceManager.getVoices();

			log.info("possible voices");
			for (int i = 0; i < possibleVoices.length; ++i) {
				log.info(possibleVoices[i].getName());
			}
			voiceName = "kevin16";
			myVoice = voiceManager.getVoice(voiceName);

			if (myVoice == null) {
				error("Cannot find a voice named " + voiceName + ".  Please specify a different voice.");
				return;
			} else {
				initialized = true;
			}
		}

		try {
			// TODO - do pre-speak not here if (!myVoice.isLoaded())
			myVoice.allocate();
			log.info("voice allocated");
		} catch (Exception e) {
			Logging.logError(e);
		}

		if (initialized) {
			invoke("isSpeaking", true);
			invoke("saying", toSpeak);
			myVoice.speak(toSpeak);
			invoke("isSpeaking", false);
		} else {
			log.error("can not speak - uninitialized");
		}

	}

	public void speakGoogle(String toSpeak) {

		if (!fileCacheInitialized) {
			boolean success = (new File("audioFile/google/" + language + "/" + voiceName)).mkdirs();
			if (!success) {
				log.debug("could not create directory: audioFile/google/" + language + "/" + voiceName);
			} else {
				fileCacheInitialized = true;
			}
		}

		// Sanitize the filename so it can be properly cached.
		toSpeak = cleanFilename(toSpeak);
		String utteranceHash = hashFilename(toSpeak);
		addToUtteranceMapping(toSpeak, utteranceHash);
		
		String audioFileName = "audioFile/google/" + language + "/" + voiceName + "/" + utteranceHash + ".mp3";
		File f = new File(audioFileName);
		log.info(f + (f.exists() ? " is found " : " is missing "));

		if (!f.exists()) {
			try {
				// if the mp3 file does not exist fetch it from google
				/*
				 * HashMap<String, String> params = new HashMap<String,
				 * String>(); params.put("voice", voiceName); params.put("txt",
				 * toSpeak); params.put("speakButton", "SPEAK");
				 */
				// rel="noreferrer"
				// http://translate.google.com/translate_tts?tl=en&q=text
				// http://translate.google.com/translate_tts?tl=fr&q=Bonjour
				// http://translate.google.com/translate_tts?tl=en&q=hello%20there%20my%20good%20friend

				client = new DefaultHttpClient();

				if (googleProxyHost != null) {
					HttpHost proxy = new HttpHost(googleProxyHost, googleProxyPort);
					client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				}

				String baseURI = String.format(googleURI, language);
				// URI uri = new URI("http", null, "translate.google.com", 80,
				// "/translate_tts", "tl=" + language + "&q=" + toSpeak, null);
				// URI uri = new URI(baseURI + URLEncoder.encode(toSpeak,
				// "ISO-8859-1"));
				log.info(baseURI + toSpeak.replaceAll(" ", "%20"));
				URI uri = new URI(baseURI + toSpeak.replaceAll(" ", "%20"));

				log.info(uri.toASCIIString());
				// HTTPClient.HTTPData data =
				// HTTPClient.get(uri.toASCIIString());

				HttpGet request = new HttpGet(uri.toASCIIString());
				HttpResponse response = client.execute(request);

				byte[] data = getByteArrayFromResponse(response);

				FileOutputStream fos = new FileOutputStream(audioFileName);
				fos.write(data);

			} catch (Exception e) {
				Logging.logError(e);
			}

		}

		invoke("isSpeaking", true);
		invoke("saying", toSpeak);
		audioFile.playFile(audioFileName, true);
		sleep(afterSpeechPause);// important pause after speech
		invoke("isSpeaking", false);
	}

	private void addToUtteranceMapping(String toSpeak, String utteranceHash) {
		// TODO : persist this somewhere other than the log file...
		log.info("Utterance Map: {} = \"{}\"", utteranceHash , toSpeak);
	}
	private String hashFilename(String toSpeak) {
		// TODO Auto-generated method stub
		String digest = DigestUtils.md5Hex(toSpeak);
		return digest;
	}
	
	public boolean speakNormal(String toSpeak) {
		// idealy in "normal" speech our ideas are queued
		// until we have time to actually say them

		if (backendType == BackendType.ATT) {
			// in(createMessage(name, "speakATT", toSpeak));
			log.error("no longer supported as per the deathstar's liscense agreement");
		} else if (backendType == BackendType.FREETTS) { // festival tts
			// speakFreeTTS(toSpeak);
			in(createMessage(getName(), "speakFreeTTS", toSpeak));
		} else if (backendType == BackendType.GOOGLE) { // festival tts
			in(createMessage(getName(), "speakGoogle", toSpeak));
		} else {
			log.error("back-end speech backendType " + backendType + " not supported ");
			return false;
		}

		return true;

	}

	@Override
	public void startService() {
		super.startService();
		startPeer("audioFile");
	}

	@Override
	public void stopService() {
		if (myVoice != null && myVoice.isLoaded()) {
			myVoice.deallocate();
		}
		if (audioFile != null) {
			audioFile.stopService();
			audioFile = null;
		}
		super.stopService();
	}

	@Override
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		Speech mouth = (Speech) Runtime.start(getName(), "Speech");
		//mouth.speak("Light scattering is a form of scattering in which light is the form of propagating energy which is scattered. Light scattering can be thought of as the deflection of a ray from a straight path, for example by irregularities in the propagation medium, particles, or in the interface between two media. Deviations from the law of reflection due to irregularities on a surface are also usually considered to be a form of scattering. When these irregularities are considered to be random and dense enough that their individual effects average out, this kind of scattered reflection is commonly referred to as diffuse reflection.");
		//mouth.speak("hello");
		mouth.speak("I don't use appostrophes, or other punctuation, do you?");
		mouth.speak("I'm done with this test");
		mouth.speak("I'm done with this test again");
		// TODO non-blocking - blocking google freetts
		status.addInfo("done with test");
		return status;
	}

	// speak errors
	
	public void setVolume(float volume) {
		// track the current volume, 
		// also realtime update the volume of the audio file.
		if (audioFile != null) {
			audioFile.setVolume(volume);
		}
	}
	
	public float getVolume() {
		if (audioFile != null) {
			return audioFile.getVolume();
		} else {
			return 1.0F;
		}
	}	
}
