package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface TextListener extends NameProvider {

  public void onText(String text) throws Exception;

  /**
   * Attach a text listener
   * 
   * @param service
   */
  default public void attachTextPublisher(TextPublisher service) {
    attachTextPublisher(service.getName());
  }

  /**
   * Default way to attach an utterance listener so implementing classes need
   * not worry about these details.
   * 
   * @param name
   */
  default public void attachTextPublisher(String name) {
    send(name, "attachTextListener", getName());
  }

  public void send(String name, String method, Object... data);
}
