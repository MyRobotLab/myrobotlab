package org.myrobotlab.service.interfaces;

import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceStatus;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * SpeechSynthesis - This is the interface that services that provide text to
 * speech should implement.
 * 
 */
public interface SpeechSynthesis extends NameProvider, ServiceStatus {

  public final static Logger log = LoggerFactory.getLogger(SpeechSynthesis.class);

  public String getlastUtterance();

  /**
   * set the speaker voice
   * 
   * @param voice
   * @return
   */
  public boolean setVoice(String voice);

  /**
   * Begin speaking something and return immediately
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @return TODO
   * @throws Exception
   *           e
   */
  public List<AudioData> speak(String toSpeak) throws Exception;

  /**
   * Begin speaking and wait until all speech has been played back/
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @throws Exception
   *           e
   * @return true/false
   */
  public List<AudioData> speakBlocking(String toSpeak) throws Exception;

  /**
   * Change audioData volume
   * 
   * @param volume
   *          - double between 0 & 1.
   */
  public void setVolume(double volume);

  /**
   * Get audioData volume
   * 
   * @return double
   */
  public double getVolume();

  /**
   * Get current voice
   * 
   * @return Voice
   */
  public Voice getVoice();

  /**
   * get voice effects on a remote server
   * 
   * @return list
   */
  // public List<String> getVoiceEffectFiles();

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   * 
   * @param utterance
   *          text
   * @return the same text
   */
  public String publishStartSpeaking(String utterance);

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   * 
   * @param utterance
   *          text
   * @return text
   */
  public String publishEndSpeaking(String utterance);

  // FIXME - not needed in interface
  // public String getLocalFileName(SpeechSynthesis provider, String toSpeak)
  // throws UnsupportedEncodingException;

  // FIXME addSpeechRecognizer
  public void addEar(SpeechRecognizer ear);

  // FIXME - is this in the wrong place ??? - this seems like bot logic ...
  public void onRequestConfirmation(String text);

  public List<Voice> getVoices();

  // public void setSelectedEffect(String effect);

  // public String getSelectedEffect();

  // FIXME - need a plan for standardization ...
  /**
   * Apply special audio effects Used for MarySpeech only for now
   * 
   * @param audioEffects
   *          text
   */
  // public void setAudioEffects(String audioEffects);

  // public String getAudioEffects();

  // public void setEffectsList(String effect, String parameters);

  // public HashMap<String, String> getEffectsList();

}