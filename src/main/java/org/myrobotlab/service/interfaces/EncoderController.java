package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

public interface EncoderController extends Attachable {

  /**
   * minimal attach
   * 
   * @param control
   *          the encoder to attach
   * 
   */
  public void attachEncoderControl(EncoderControl control);

  // possibly ? for ones which support it ?
  // > setZeroPoint/deviceId
  // public void setZeroPoint(Integer deviceId);

  // < publishEncoderPosition/deviceId/b16 position
  public EncoderData publishEncoderData(EncoderData data);

  public void setZeroPoint(EncoderControl encoder);

}
