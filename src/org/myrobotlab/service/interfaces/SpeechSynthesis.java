package org.myrobotlab.service.interfaces;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * SpeechSynthesis - This is the interface that services that provide text to
 * speech should implement.
 * 
 */
public interface SpeechSynthesis extends NameProvider {
  
  public final static Logger log = LoggerFactory.getLogger(SpeechSynthesis.class);

  public abstract List<String> getVoices();

  public boolean setVoice(String voice);

  public abstract void setLanguage(String l);

  public abstract String getLanguage();

  /**
   * @return a list of current possible languages
   */
  public abstract List<String> getLanguages();

  /**
   * Begin speaking something and return immediately
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @return TODO
   * @throws Exception e
   */
  public abstract AudioData speak(String toSpeak) throws Exception;
  
  /**
   * Begin speaking and wait until all speech has been played back/
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @throws Exception e
   * @return true/false
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
   * @param utterance text
   * @return the same text
   */
  public String publishStartSpeaking(String utterance);

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   * @param utterance text
   * @return text
   */
  public String publishEndSpeaking(String utterance);

  public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException;

  public void addEar(SpeechRecognizer ear);

  public void onRequestConfirmation(String text);

}