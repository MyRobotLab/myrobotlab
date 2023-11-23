package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AudioData;

public interface AudioListener extends NameProvider {

  public void onAudioStart(AudioData data);

  public void onAudioEnd(AudioData data);

}
