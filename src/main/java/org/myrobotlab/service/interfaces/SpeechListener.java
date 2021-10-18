package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This interface listens to speech
 * 
 * @author GroG
 *
 */
public interface SpeechListener extends NameProvider {

  public final static Logger log = LoggerFactory.getLogger(SpeechListener.class);

  /**
   * speech has begun with the this utterance
   * 
   * @param utterance
   */
  public void onStartSpeaking(String utterance);

  /**
   * speech has ended with the this utterance
   * 
   * @param utterance
   */
  public void onEndSpeaking(String utterance);
}