package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

public interface EncoderControl extends Attachable {

  public void attach(EncoderController controller, Integer pin) throws Exception;

  public void setController(EncoderController controller);

  // TODO: publish encoder data that includes the name/id of the device.
  public void onEncoderData(EncoderData data);
  
  /**
   * for address or pin names - such as D0, D1, A0, ....
   * at some point the controller will "probably" convert this to an actual address
   * @param address
   */
  public void setPin(String address);
  
  /**
   * integer address or pin number
   * @param address
   */
  public void setPin(int address);

  // the cable select pin that enables this encoder.
  public String getPin();

}
