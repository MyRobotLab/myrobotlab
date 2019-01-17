package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.AudioData;

public interface AudioListener {

  public void onAudioStart(AudioData data);

  public void onAudioEnd(AudioData data);

}
