package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.maryspeech.tools.install.MaryInstaller;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

public class MarySpeech extends AbstractSpeechSynthesis implements TextListener {

	public final static Logger log = LoggerFactory.getLogger(MarySpeech.class);
	private static final long serialVersionUID = 1L;

	private transient MaryInterface marytts = null;
	private String language;

	// we need to subclass the audio player class here, so we know when the run
	// method exits and we can invoke
	// publish end speaking from it.
	private class MRLAudioPlayer extends AudioPlayer {

		private final String utterance;

		public MRLAudioPlayer(AudioInputStream ais, String utterance) {
			super(ais);
			this.utterance = utterance;
		}

		@Override
		public void run() {
			invoke("publishStartSpeaking", utterance);
			// give a small pause for sphinx to stop listening?
			try {
				Thread.sleep(100);
				log.info("Ok.. here we go.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			super.run();

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			invoke("publishEndSpeaking", utterance);
		}
	}

	@Override
	public boolean speakBlocking(String toSpeak) throws SynthesisException, InterruptedException {
		return speakInternal(toSpeak, true);
	}

	public boolean speakInternal(String toSpeak, boolean blocking) throws SynthesisException, InterruptedException {
		AudioInputStream audio;

		log.info("speakInternal Blocking {} Text: {}", blocking, toSpeak);
		if (toSpeak == null || toSpeak.length() == 0) {
			log.info("speech null or empty");
			return false;
		}
		audio = marytts.generateAudio(toSpeak);
		// invoke("publishStartSpeaking", toSpeak);

		MRLAudioPlayer player = new MRLAudioPlayer(audio, toSpeak);
		// player.setAudio(audio);
		player.start();
		// To make this blocking you can join the player thread.
		if (blocking) {
			player.join();
		}
		// TODO: if this isn't blocking, we might just return immediately,
		// rather
		// than
		// saying when the player has finished.
		// invoke("publishEndSpeaking", toSpeak);
		return true;

	}

	public MarySpeech(String reservedKey) {
		super(reservedKey);

		File file = new File("mary");
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File("mary\\download");
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File("mary\\installed");
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File("mary\\lib");
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File("mary\\log");
		if (!file.exists()) {
			file.mkdirs();
		}
		
		String maryBase = "mary";

		System.setProperty("mary.base", maryBase);
		System.setProperty("mary.downloadDir", new File(maryBase + "/download").getPath());
		System.setProperty("mary.installedDir", new File(maryBase + "/installed").getPath());

		try {
			marytts = new LocalMaryInterface();
		} catch (Exception e) {
			Logging.logError(e);
		}

		// Grab the first voice that's available. :-/
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());

	}

	@Override
	public void onText(String text) {
		log.info("ON Text Called: {}", text);
		try {
			speak(text);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public AudioData speak(String toSpeak) throws SynthesisException, InterruptedException {
		AudioData ret = new AudioData(toSpeak);
		// TODO: handle the isSpeaking logic/state
		speakInternal(toSpeak, false);
		// FIXME - play cache track
		return ret;
	}

	@Override
	public List<String> getVoices() {
		List<String> list = new ArrayList<>(marytts.getAvailableVoices());
		return list;
	}

	@Override
	public boolean setVoice(String voice) {
		marytts.setVoice(voice);
		return true; // setVoice is void - if voice isn't available it throws an
						// exception
	}

	@Override
	public void setLanguage(String l) {
	  if (!l.equalsIgnoreCase("en"))
	  {
		marytts.setLocale(Locale.forLanguageTag(l));
	  }
		this.language=l;
	}

	@Override
	public void onRequestConfirmation(String text) {
		try {
			// FIXME - not exactly language independent
			speakBlocking(String.format("did you say. %s", text));
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public String getLanguage() {
		return marytts.getLocale().getLanguage();
	}

	@Override
	public void setVolume(float volume) {
		// TODO Auto-generated method stub

	}

	@Override
	public float getVolume() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public String publishStartSpeaking(String utterance) {
		// TODO Auto-generated method stub
		log.info("Starting to speak: {}", utterance);
		return utterance;
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		// TODO Auto-generated method stub
		log.info("End speaking: {}", utterance);
		return utterance;
	}

	@Override
	public String getVoice() {
		return marytts.getVoice();
	}

	@Override
	public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType)
			throws UnsupportedEncodingException {
		return provider.getClass().getSimpleName() + File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8")
				+ File.separator + DigestUtils.md5Hex(toSpeak) + "." + audioFileType;
	}

	@Override
	public void addEar(SpeechRecognizer ear) {
		// when we add the ear, we need to listen for request confirmation
		addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
		addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
	}

	@Override
	public List<String> getLanguages() {
		List<String> ret = new ArrayList<>();
		for (Locale locale : marytts.getAvailableLocales()) {
			ret.add(locale.getLanguage());
		}
		return ret;
	}

	public void setAudioEffects(String effects) {
		marytts.setAudioEffects(effects);
	}
	
	public void installComponentsAcceptLicense(String component) {
		installComponentsAcceptLicense(new String[]{component});
	}
	
	public void installComponentsAcceptLicense(String[] components) {
		if (components == null) {
			return;
		}
		org.myrobotlab.maryspeech.tools.install.MaryInstaller installer = new MaryInstaller("https://raw.github.com/marytts/marytts/master/download/marytts-components.xml");
		Map<String, org.myrobotlab.maryspeech.tools.install.LanguageComponentDescription> languages = installer.getLanguages();
		Map<String, org.myrobotlab.maryspeech.tools.install.VoiceComponentDescription> voices = installer.getVoices();
		
		List<org.myrobotlab.maryspeech.tools.install.ComponentDescription> toInstall = new ArrayList<>();
		for (String component : components) {
			if (component == null || component.isEmpty() || component.trim().isEmpty()) {
				continue;
			}
			if (languages.containsKey(component)) {
				toInstall.add(languages.get(component));
			} else if (voices.containsKey(component)) {
				toInstall.add(voices.get(component));
			} else {
				log.warn("can't find component for installation");
			}
		}
		
		log.info("starting marytts component installation:" + toInstall);
		installer.installSelectedLanguagesAndVoices(toInstall);
		log.info("moving files to correct places ...");
		File srcDir = new File(System.getProperty("mary.base") + "/lib");
		File destDir = new File ("libraries/jar");
		try {
			FileUtils.copyDirectory(srcDir, destDir);
			log.info("finished marytts component installation");
			log.info("PLEASE RESTART TO APPLY CHANGES !!!");
		} catch (IOException e) {
			log.error("moving files FAILED!");
		}
	}

	public static void main(String[] args) {
		LoggingFactory.init(Level.DEBUG);

		try {
			//Runtime.start("webgui", "WebGui");
			MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");

			// examples are generously copied from
			// marytts.signalproc.effects.EffectsApplier.java L319-324
			// String strEffectsAndParams = "FIRFilter+Robot(amount=50)";
			String strEffectsAndParams = "Robot(amount=100)+Chorus(delay1=866, amp1=0.24, delay2=300, amp2=-0.40,)";
			// String strEffectsAndParams =
			// "Robot(amount=80)+Stadium(amount=50)";
			// String strEffectsAndParams = "FIRFilter(type=3,fc1=6000,
			// fc2=10000) + Robot";
			// String strEffectsAndParams = "Stadium(amount=40) +
			// Robot(amount=87) +
			// Whisper(amount=65)+FIRFilter(type=1,fc1=1540;)++";
			mary.setAudioEffects(strEffectsAndParams);

			// mary.setVoice("dfki-spike en_GB male unitselection general");
			// mary.speak("hello");
			// mary.speak("world");
			//mary.speakBlocking("Hello world");
			// mary.speakBlocking("I am Mary TTS and I am open source");
			// mary.speakBlocking("and I will evolve quicker than any closed
			// source
			// application if not in a short window of time");
			// mary.speakBlocking("then in the long term evolution of
			// software");
			// mary.speak("Hello world");
			mary.installComponentsAcceptLicense("bits1");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(MarySpeech.class.getCanonicalName());
		meta.addDescription("Speech synthesis based on MaryTTS");
		meta.addCategory("speech", "sound");
		meta.addDependency("marytts", "5.2");
		meta.addDependency("com.sun.speech.freetts", "1.2");
		meta.addDependency("opennlp", "1.6");
		return meta;
	}

}
