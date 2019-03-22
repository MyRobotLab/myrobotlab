package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

public interface EncoderListener extends Attachable {

  // TODO: publish encoder data that includes the name/id of the device.
  public void onEncoderData(EncoderData data);
  
}
