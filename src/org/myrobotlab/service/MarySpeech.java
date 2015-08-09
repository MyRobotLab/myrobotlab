package org.myrobotlab.service;

import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

public class MarySpeech extends Service implements TextListener {

	public final static Logger log = LoggerFactory.getLogger(MarySpeech.class.getCanonicalName());
	
	private MaryInterface marytts = null;
	
	public MarySpeech(String reservedKey) {
		super(reservedKey);
	
		// 
		try {
			marytts = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			// TODO Auto-generated catch block
			log.error("Mary TTS Error:" , e);
			e.printStackTrace();
		}
		// Grab the first voice that's available.  :-/  
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 8474747248503262499L;



	@Override
	public void onText(String text) {
		// TODO Auto-generated method stub
		speak(text);
	}
	
	public boolean speak(String toSpeak) {
		// TODO: handle the isSpeaking logic/state
		return speakInternal(toSpeak, false);
	}

	public boolean speakBlocking(String toSpeak) {
		return speakInternal(toSpeak, true);
	}
	
	public boolean speakInternal(String toSpeak, boolean blocking) {
		AudioInputStream audio;
		try {
			audio = marytts.generateAudio(toSpeak);
			AudioPlayer player = new AudioPlayer(audio);
			player.start();
			// To make this blocking you can join the player thread.
			if (blocking) {
				player.join();
			}
			return true;
		} catch (SynthesisException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("Speech synth failed", e);
			return false;
		}

	}

	@Override
	public String[] getCategories() {
		return new String[] { "speech", "sound" };
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Speech synthesis based on MaryTTS";
	}
	
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");
		mary.speakBlocking("Hello world");
		mary.speak("Hello world");
	};

}
