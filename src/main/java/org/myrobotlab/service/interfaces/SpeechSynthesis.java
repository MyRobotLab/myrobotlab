package org.myrobotlab.service.interfaces;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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

  public abstract String getlastUtterance();

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
   * @throws Exception
   *           e
   */
  public abstract AudioData[] speak(String toSpeak) throws Exception;

  /**
   * Begin speaking and wait until all speech has been played back/
   * 
   * @param toSpeak
   *          - the string of text to speak.
   * @throws Exception
   *           e
   * @return true/false
   */
  public abstract boolean speakBlocking(String toSpeak) throws Exception;

  /**
   * Change audioData volume
   * 
   * @param volume
   *          - float between 0 & 1.
   */
  public abstract void setVolume(float volume);

  /**
   * Get audioData volume
   * 
   * @return float
   */
  public abstract float getVolume();

  /**
   * Get current voice
   * 
   * @return String
   */
  public abstract String getVoice();

  /**
   * get voice effects on a remote server
   * 
   * @return list
   */
  public abstract List<String> getVoiceEffects();

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   * 
   * @param utterance
   *          text
   * @return the same text
   */
  public abstract String publishStartSpeaking(String utterance);

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   * 
   * @param utterance
   *          text
   * @return text
   */
  public abstract String publishEndSpeaking(String utterance);

  public abstract String getLocalFileName(SpeechSynthesis provider, String toSpeak) throws UnsupportedEncodingException;

  public abstract void addEar(SpeechRecognizer ear);

  public abstract void onRequestConfirmation(String text);

  /**
   * use tts engine to create an audiofile
   * 
   * @param toSpeak
   *          text
   * @return byte[]
   */
  public byte[] generateByteAudio(String toSpeak) throws IOException;

  /**
   * check if all is OK, like api keys
   * 
   * @return boolean
   */
  public abstract boolean getEngineStatus();

  public abstract String getEngineError();

  public abstract void setEngineStatus(boolean engineStatus);

  public abstract void setEngineError(String engineError);

  /**
   * set api keys for online tts and store them inside an aes safe
   * 
   * @param keyId
   *          ,keyIdSecret text,text
   */
  public void setKeys(String keyId, String keyIdSecret);

  public String[] getKeys();

  public String getVoiceInJsonConfig();

  public void setVoiceInJsonConfig(String voice);

  public abstract String getAudioCacheExtension();

  public abstract void setAudioCacheExtension(String audioCacheExtension);

  public abstract List<String> getVoiceList();

  public abstract void setVoiceList(List<String> voiceList);

  public abstract void setSelectedEffect(String effect);

  public abstract String getSelectedEffect();

  /**
   * Apply special audio effects Used for MarySpeech only for now
   * 
   * @param audioEffects
   *          text
   */
  public void setAudioEffects(String audioEffects);

  public String getAudioEffects();

  public abstract void setEffectsList(String effect, String parameters);

  public abstract HashMap<String, String> getEffectsList();

}