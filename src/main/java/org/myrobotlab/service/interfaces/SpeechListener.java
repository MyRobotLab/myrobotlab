package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

/**
 * This interface listens to speech
 * 
 * @author GroG
 *
 */
public interface SpeechListener extends NameProvider {

  /**
   * speech has begun with the this utterance
   * 
   * @param utterance
   *          - the speech that fragment was started in text form
   */
  public void onStartSpeaking(String utterance);

  /**
   * speech has ended with the this utterance
   * 
   * @param utterance
   *          - the speech fragement that was finished
   */
  public void onEndSpeaking(String utterance);
}