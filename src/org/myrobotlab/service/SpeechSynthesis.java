package org.myrobotlab.service;

import java.util.List;

/**
 * SpeechSynthesis
 * 
 */
public interface SpeechSynthesis {

	// kept for legacy - use publishStartSpeaking & publishEndSpeaking
	// for event handlers  - previously isSpeaking event would happen with a true or false
	public abstract Boolean isSpeaking(Boolean b);

	public abstract List<String> getVoices();

	public boolean setVoice(String voice);

	// kept for legacy - use publishStartSpeaking & publishEndSpeaking
	// for event handlers - previously speak event would happen with the 
	// utterance
	public abstract String saying(String t);

	public abstract void setLanguage(String l);

	public abstract String getLanguage();

	public abstract boolean speak(String toSpeak);

	public abstract boolean speakBlocking(String toSpeak);

	public abstract void setVolume(float volume);

	public abstract float getVolume();

	public abstract void interrupt();
	
	public String getVoice();

	// start/stop callbacks for speech synth.
	public abstract String publishStartSpeaking(String utterance);

	public abstract String publishEndSpeaking(String utterance);
	
	//public boolean speakQueued(String toSpeak);

}