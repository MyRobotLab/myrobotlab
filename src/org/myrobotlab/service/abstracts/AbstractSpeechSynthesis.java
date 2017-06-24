package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesis extends Service implements SpeechSynthesis {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesis.class);


  public AbstractSpeechSynthesis(String reservedKey) {
    super(reservedKey);
  }

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   * 
   * @param utterance
   * @return
   */
  public String publishStartSpeaking(String utterance){
    log.info("publishStartSpeaking - {}", utterance);
    return utterance;
  }

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   * 
   * @param utterance
   * @return
   */
  public String publishEndSpeaking(String utterance){
    log.info("publishEndSpeaking - {}", utterance);
    return utterance;
  }

}
