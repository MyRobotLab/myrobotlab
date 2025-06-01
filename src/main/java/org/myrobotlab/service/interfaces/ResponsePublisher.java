package org.myrobotlab.service.interfaces;

import org.myrobotlab.programab.Response;

/**
 * Response Publisher interface is an interface that publishes responses. An
 * response is a piece of text that came from a user. It might come from a chat
 * channel like discord, or from a speech recognizer.
 *
 * 
 */
public interface ResponsePublisher {

  // These are all the methods that the response publisher should produce.
  public static String[] publishMethods = new String[] { "publishResponse" };
  // FIXME - this should be promoted and inherited ^

  // Define the methods that an response publisher should have
  public Response publishResponse(Response response);

  // Default way to attach an response listener so implementing classes need
  // not worry about these details.
  default public void attachResponseListener(String name) {
    for (String publishMethod : ResponsePublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);

}
