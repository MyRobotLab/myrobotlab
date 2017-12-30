package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextPublisher;

public abstract class AbstractSpeechRecognizer extends Service implements SpeechRecognizer, TextPublisher {

  private static final long serialVersionUID = 1L;

  public AbstractSpeechRecognizer(String reservedKey) {
    super(reservedKey);
  }
  
  /**
   * routable attach handles attaching based on type info
   */
  public void attach(Attachable attachable){
    if (attachable instanceof SpeechSynthesis){
      addMouth((SpeechSynthesis)attachable);
    } else {
      error("do not know how to attach %s", attachable.getName());
    }
  }

}
