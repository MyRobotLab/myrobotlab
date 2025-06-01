package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface TextPublisher extends NameProvider {

  /**
   * These are all the methods that the utterance publisher should produce.
   */
  public static String[] publishMethods = new String[] { "publishText" };

  /**
   * Define the methods that an utterance publisher should have
   * 
   * @param text
   * @return
   */
  public String publishText(String text);

  /**
   * Attach a text listener
   * 
   * @param service
   */
  default public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  /**
   * Default way to attach an utterance listener so implementing classes need
   * not worry about these details.
   * 
   * @param name
   */
  default public void attachTextListener(String name) {
    for (String publishMethod : TextPublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  default public void detachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  default public void detachTextListener(String name) {
    for (String publishMethod : TextPublisher.publishMethods) {
      removeListener(publishMethod, name);
    }
  }

  /**
   * Add the addListener method to the interface all services implement this.
   * 
   * @param topicMethod
   * @param callbackName
   */
  public void addListener(String topicMethod, String callbackName);

  public void removeListener(String topicMethod, String callbackName);

}
