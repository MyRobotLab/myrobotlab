package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

public class MarySpeech extends Service implements TextListener, SpeechSynthesis {

	public final static Logger log = LoggerFactory.getLogger(MarySpeech.class);
	private static final long serialVersionUID = 1L;
	
	MaryInterface marytts = null;
	
	public MarySpeech(String reservedKey) {
		super(reservedKey);

		try {
			marytts = new LocalMaryInterface();
		} catch (Exception e) {
			Logging.logError(e);
		}
		
		// Grab the first voice that's available.  :-/  
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());
		
	}

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
	
	
	// TODO IMPLEMENT !!!! --------- WHEEEEE !

	@Override
	public Boolean isSpeaking(Boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getVoices() {
		List<String> list = new ArrayList<String>(marytts.getAvailableVoices());
		return list;
	}

	@Override
	public boolean setVoice(String voice) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String saying(String t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLanguage(String l) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getLanguage() {
		// TODO Auto-generated method stub
		return null;
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
		return null;
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		// TODO Auto-generated method stub
		return null;
	};
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");
		mary.speakBlocking("Hello world");
		mary.speakBlocking("I am Mary TTS and I am open source");
		mary.speakBlocking("and I will evolve quicker than any closed source application if not in a short window of time");
		mary.speakBlocking("then in the long term evolution of software");
		mary.speak("Hello world");
	}

	@Override
	public String getVoice() {
		return marytts.getVoice();
	}

	
}
