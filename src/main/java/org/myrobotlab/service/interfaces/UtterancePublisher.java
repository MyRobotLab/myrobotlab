package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.Utterance;

/**
 * Utterance Publisher interface is an interface that publishes utterances. An
 * utterance is a piece of text that came from a user. It might come from a chat
 * channel like discord, or from a speech recognizer.
 *
 * 
 */
public interface UtterancePublisher extends NameProvider {

  // These are all the methods that the utterance publisher should produce.
  public static String[] publishMethods = new String[] { "publishUtterance" };
  // FIXME - this should be promoted and inherited ^

  // Define the methods that an utterance publisher should have
  public Utterance publishUtterance(Utterance utterance);

  // Default way to attach an utterance listener so implementing classes need
  // not worry about these details.
  default public void attachUtteranceListener(String name) {
    for (String publishMethod : UtterancePublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }
  
  default public void detachUtteranceListener(UtteranceListener display) {
    detachUtteranceListener(display.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void detachUtteranceListener(String name) {
    for (String publishMethod : UtterancePublisher.publishMethods) {
      removeListener(publishMethod, name);
    }
  }


  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);
  
  public void removeListener(String topicMethod, String callbackName);


}
