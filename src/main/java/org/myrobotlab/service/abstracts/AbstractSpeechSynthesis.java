package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public abstract class AbstractSpeechSynthesis extends Service implements SpeechSynthesis {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AbstractSpeechSynthesis.class);


  public AbstractSpeechSynthesis(String reservedKey) {
    super(reservedKey);
  }

  /**
   * start callback for speech synth. (Invoked when speaking starts)
   */
  public String publishStartSpeaking(String utterance){
    log.info("publishStartSpeaking - {}", utterance);
    return utterance;
  }

  /**
   * stop callback for speech synth. (Invoked when speaking stops.)
   */
  public String publishEndSpeaking(String utterance){
    log.info("publishEndSpeaking - {}", utterance);
    return utterance;
  }
  
  /**
   * attach method responsible for routing to type-mangled attach
   */
  public void attach(Attachable attachable){
    if (attachable instanceof TextPublisher){
      attachTextPublisher((TextPublisher)attachable);
    } else {
      log.error("don't know how to attach a %s", attachable.getName());
    }
  }
  
  /**
   * detach method responsible for routing to type-mangled attach
   */
  public void detach(Attachable attachable){
    if (attachable instanceof TextPublisher){
      detachTextPublisher((TextPublisher)attachable);
    }
  }
  
  public void attachTextPublisher(TextPublisher textPublisher){
    subscribe(textPublisher.getName(), "publishText");
    // FIXME -
    // if (!isAttached(textPublisher.getName())){
    //      textPublisher.attach(this) ???
  }
  
  public void detachTextPublisher(TextPublisher textPublisher){
    unsubscribe(textPublisher.getName(), "publishText");
  }


}
