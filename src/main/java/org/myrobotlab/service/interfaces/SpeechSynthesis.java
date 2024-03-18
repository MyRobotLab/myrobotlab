package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.WordFilter;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * SpeechSynthesis - This is the interface that services that provide text to
 * speech should implement.
 * 
 */
public interface SpeechSynthesis extends NameProvider, TextListener, LocaleProvider {

  public final static Logger log = LoggerFactory.getLogger(SpeechSynthesis.class);

  public String getlastUtterance();

  /**
   * set the speaker voice
   * 
   * @param voice
   *          name of voice to set.
   * @return success or failure
   * 
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
   *          - double between 0 and 1.
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

  /**
   * silence the service
   */
  @Deprecated /* use setMute */
  public void mute();

  /**
   * un-silence the service
   */
  @Deprecated /* use setMute */
  public void unmute();

  /**
   * mute or unmute
   * 
   * @param mute
   *          true to mute
   * 
   */
  public void setMute(boolean mute);

  @Deprecated /*
               * this should be type specific named - use attachSpeechRecognizer
               */
  public void addEar(SpeechRecognizer ear);

  // FIXME - is this in the wrong place ??? - this seems like bot logic ...
  public void onRequestConfirmation(String text);

  /**
   * @return get a list of voices this speech synthesis supports
   * 
   * 
   */
  public List<Voice> getVoices();

  /**
   * puts all speaking into blocking mode - default is false
   * 
   * @param b
   *          true to block
   * @return blocking value
   * 
   */
  public Boolean setBlocking(Boolean b);

  /**
   * This attach subscribes the the SpeechRecognizer to the SpeechSynthesizer so
   * the bot won't incorrectly recognize itself when its speaking ... otherwise
   * silly things can happen when talking to self...
   * 
   * @param ear
   *          to attach
   */
  public void attachSpeechRecognizer(SpeechRecognizer ear);

  /**
   * Speech control controls volume, setting the voice, and of course "speak"
   * 
   * @param control
   *          the speech synth to attach
   * 
   */
  public void attachSpeechControl(SpeechSynthesisControl control);

  /**
   * These are the methods that a speech listener should subscribe to.
   */
  public static String[] publishSpeechListenerMethods = new String[] { "publishStartSpeaking", "publishEndSpeaking" };

  /**
   * Attach a speech listener which gets on started/stopped speaking callbacks.
   * 
   * @param name
   */
  default public void attachSpeechListener(String name) {
    for (String method : publishSpeechListenerMethods) {
      addListener(method, name);
    }
  }

  /**
   * Detach a speech listener that will remove the listeners for the speech
   * listener methods.
   * 
   * @param name
   */
  default public void detachSpeechListener(String name) {
    for (String method : publishSpeechListenerMethods) {
      removeListener(method, name);
    }
  }

  // All services implement this.
  public void addListener(String topicMethod, String callbackName);

  // All services implement this.
  public void removeListener(String topicMethod, String callbackName);

  /**
   * replace one word with another - instead of "biscuit" say "cookie"
   * 
   * @param key
   *          lookup word
   * @param replacement
   *          replacement word.
   * 
   */
  public void replaceWord(String key, String replacement);

  /**
   * replace one word with another - instead of "biscuit" say "cookie"
   * 
   * @param filter
   *          word filter to use
   */
  public void replaceWord(WordFilter filter);
  
  /**
   * Stops speaking
   */
  public void stop();
  
}