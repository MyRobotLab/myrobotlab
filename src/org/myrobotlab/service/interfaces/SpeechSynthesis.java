package org.myrobotlab.service.interfaces;

import java.util.List;

/**
 * SpeechSynthesis - This is the interface that services that provide text to speech should implement. 
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

	/**
	 * Begin speaking something and return immediately
	 * @param toSpeak - the string of text to speak.
	 * @return
	 */
	public abstract boolean speak(String toSpeak);

	/**
	 * Begin speaking and wait until all speech has been played back/
	 * @param toSpeak - the string of text to speak.
	 * @return
	 */
	public abstract boolean speakBlocking(String toSpeak);

	public abstract void setVolume(float volume);

	public abstract float getVolume();

	/**
	 * Interrupt the current speaking.
	 */
	public abstract void interrupt();
	
	public String getVoice();

	/**
	 * start callback for speech synth. (Invoked when speaking starts)
	 * @param utterance
	 * @return
	 */
	public abstract String publishStartSpeaking(String utterance);

	/**
	 * stop callback for speech synth. (Invoked when speaking stops.) 
	 * @param utterance
	 * @return
	 */
	public abstract String publishEndSpeaking(String utterance);
	
	//public boolean speakQueued(String toSpeak);

}