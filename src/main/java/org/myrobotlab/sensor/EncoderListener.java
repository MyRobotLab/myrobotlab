package org.myrobotlab.sensor;

import org.myrobotlab.framework.interfaces.Attachable;

public interface EncoderListener extends Attachable {

  void onEncoderData(EncoderData data);

}
