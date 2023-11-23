package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AudioData;

public interface AudioPublisher extends NameProvider {

  public static String[] publishMethods = new String[] { "publishAudioStart", "publishAudioEnd" };

  public AudioData publishAudioStart(AudioData data);

  public AudioData publishAudioEnd(AudioData data);

  // Default way to attach an utterance listener so implementing classes need
  // not worry about these details.
  default public void attachAudioListener(String name) {
    for (String publishMethod : AudioPublisher.publishMethods) {
      addListener(publishMethod, name);
    }
  }

  // Add the addListener method to the interface all services implement this.
  public void addListener(String topicMethod, String callbackName);

}
