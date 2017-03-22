package org.myrobotlab.service.interfaces;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.myrobotlab.service.data.AudioData;

/**
 * SpeechSynthesis - This is the interface that services that provide text to
 * speech should implement.
 * 
 */
public interface SpeechSynthesis extends NameProvider {

  public abstract List<String> getVoices();

  public boolean setVoice(String voice);

  public abstract void setLanguage(String l);

  public abstract String getLanguage();

  /**
   * returns a list of current possible languages
   * 
   * @return
   */
  public abstract List<String> getLanguages();

  /**
   * Begin speaking something and return immediately
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @return TODO
   */
  public abstract AudioData speak(String toSpeak) throws Exception;

  /**
   * Begin speaking and wait until all speech has been played back/
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @return
   */
  public abstract boolean speakBlocking(String toSpeak) throws Exception;

  public abstract void setVolume(float volume);

  public abstract float getVolume();

  /**
   * Interrupt the current speaking.
   */
  public abstract void interrupt();

  public String getVoice();

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   * 
   * @param utterance
   * @return
   */
  public abstract String publishStartSpeaking(String utterance);

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   * 
   * @param utterance
   * @return
   */
  public abstract String publishEndSpeaking(String utterance);

  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException;

  public void addEar(SpeechRecognizer ear);

  public void onRequestConfirmation(String text);

}