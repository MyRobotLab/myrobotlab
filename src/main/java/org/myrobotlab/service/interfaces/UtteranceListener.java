package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.Utterance;

/**
 * An utterance Listener. It can be attached to by an Utterance publisher.
 *
 */
public interface UtteranceListener {

  public String getName();

  public void onUtterance(Utterance utterance) throws Exception;
  
  
  default public void attachUtterancePublisher(UtterancePublisher publisher) {
    attachUtterancePublisher(publisher.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void attachUtterancePublisher(String name) {
    send(name, "attachUtteranceListener", getName());
  }

  default public void detachUtterancePublisher(UtterancePublisher publisher) {
    detachUtterancePublisher(publisher.getName());
  }

  // Default way to attach an image listener so implementing classes need
  // not worry about these details.
  default public void detachUtterancePublisher(String name) {
    send(name, "detachUtteranceListener", getName());
  }

  public void send(String name, String method, Object... data);

}
