package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * 
 * WebkitSpeechSynthesis -
 * https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis
 *
 * @author GroG
 *
 */
public class WebkitSpeechSynthesis extends AbstractSpeechSynthesis {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesis.class);

	Map<String, Integer> nameToIndex = new HashMap<>();

	/**
	 * the index of current voice in the browser
	 */
	protected int voiceIndex = 0;

	public WebkitSpeechSynthesis(String n, String id) {
		super(n, id);

		/**
		 * speechSynthesis.speak() without user activation is no longer allowed since
		 * M71, around December 2018. See
		 * https://www.chromestatus.com/feature/5687444770914304 for more details
		 * speechSynthesisMessage
		 * 
		 * We start this service as mute until the user presses the unmute button
		 */

		setMute(true);
	}

	/**
	 * webkit currently cannot generate audio data - would be cool to download the
	 * file if possible
	 */
	@Override
	public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {
		// done in the web browser - we don't get audioData - it would be nice if we did
		// perhaps it can be downloaded ....

		// send message to browser to speak
		invoke("webkitSpeak", toSpeak);
		return null;
	}

	public String webkitSpeak(String text) {
		return text;
	}

	/**
	 * This method is called by the browser, and it populates the list of voices.
	 * 
	 * @param index
	 * @param name
	 * @param lang
	 * @param def
	 */
	public void addWebKitVoice(Integer index, String name, String lang, Boolean def) {
		nameToIndex.put(name, index);
		addVoice(name, null, lang, null);
	}

	/**
	 * This static method returns all the details of the class without it having to
	 * be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = AbstractSpeechSynthesis.getMetaData(WebkitSpeechSynthesis.class.getCanonicalName());

		meta.addDescription("used as a general template");
		meta.setAvailable(true); // false if you do not want it viewable in a
		// gui
		// add dependency if necessary
		meta.addCategory("speech", "sound");
		return meta;
	}

	public boolean setVoice(String name) {
		if (voices.containsKey(name)) {
			voice = voices.get(name);
			voiceIndex = nameToIndex.get(name);
			// invoke("publishVoiceIndex", voiceIndex);
			broadcastState();
			return true;
		}

		error("could not set voice %s - valid voices are %s", name, String.join(", ", getVoiceNames()));
		return false;
	}

	public Integer publishVoiceIndex(Integer voiceIndex) {
		return voiceIndex;
	}

	/**
	 * https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis/getVoices
	 */
	@Override
	protected void loadVoices() throws Exception {
		// done in the webbrowser - this method is a NOOP

	}

	public static void main(String[] args) {
		try {

			LoggingFactory.init(Level.INFO);
			Platform.setVirtual(true);
			Runtime.main(new String[] { "--interactive", "--id", "inmoov" });

			WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
			webgui.autoStartBrowser(false);
			webgui.startService();

			WebkitSpeechSynthesis speech = (WebkitSpeechSynthesis) Runtime.start("speech", "WebkitSpeechSynthesis");

			for (int i = 0; i < 1000; ++i) {
				speech.setVoice("Google UK English Female");
				speech.speak("how now brown cow");
				speech.setVoice("Google UK English Male");
				speech.speak("how now brown cow");
				speech.setVoice("Google français");
				speech.speak("Ah, la vache! Chercher la petite bête");
				speech.setVoice("Google Deutsch");
				speech.speak("Da liegt der Hund begraben.");
				speech.setVoice("Google Nederlands");
				speech.speak("Nu komt de aap uit de mouw");
				speech.setVoice("Google Nederlands");
				speech.speak("Nu komt de aap uit de mouw");				
				speech.setVoice("Google italiano");
				speech.speak("Ubriaco come una scimmia");
								
			}
			boolean done = true;
			if (done) {
				return;
			}

		} catch (Exception e) {
			log.error("main threw", e);
		}
	}

}
