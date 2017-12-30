package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.AudioData;

public interface AudioPublisher {

  public void publishAudioStart(AudioData data);

  public void publishAudioEnd(AudioData data);

  // hmm public void attach(AudioListener listener);

}
