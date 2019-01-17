package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

public interface EncoderControl extends Attachable {

  public void attach(EncoderController controller, Integer pin) throws Exception;

  public void setController(EncoderController controller);

  // TODO: publish encoder data that includes the name/id of the device.
  public void onEncoderData(EncoderData data);

  // the cable select pin that enables this encoder.
  public int getPin();

}
