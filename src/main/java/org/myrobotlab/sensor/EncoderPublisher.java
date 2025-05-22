package org.myrobotlab.sensor;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface EncoderPublisher extends NameProvider {
  
  // These are all the methods that the Encoder publisher should produce.
  public static String[] publishMethods = new String[] { "publishEncoderData" };
  
  // Define the methods that an Encoder publisher should have for publishing data.
  default public EncoderData publishEncoderData(EncoderData data) {
    return data;
  };

  // helper default methods to attach and detach a listener.
  default public void attachEncoderListener(String name) {
    for (String publishMethod : EncoderPublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  default public void attachEncoderListener(EncoderListener listener) {
    for (String publishMethod : EncoderPublisher.publishMethods) {
      addListener(publishMethod, listener.getName());
    }
  }

  // detach methods
  default public void detachEncoderListener(String name) {
    for (String publishMethod : EncoderPublisher.publishMethods) {
      removeListener(publishMethod, name);
    }
  }
  
  default public void detachEncoderListener(EncoderListener listener) {
    detachEncoderListener(listener.getName());
  }

  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);
  
  public void removeListener(String topicMethod, String callbackName);
  
}
