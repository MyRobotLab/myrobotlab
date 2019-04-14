package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

public interface EncoderControl extends Attachable {

  public void attach(EncoderController controller, Integer pin) throws Exception;

  /**
   * stop the stream of encoder data
   */
  public void disable();

  // TODO: publish encoder data that includes the name/id of the device.
  // public void onEncoderData(EncoderData data);

  /**
   * enable this encoder to provide a stream of encoder data
   */
  public void enable();

  /**
   * the pin the encoder is attached to (configuration for the
   * EncoderController)
   * 
   * @return
   */
  public String getPin();

  /**
   * return the state of streaming encoder data
   * 
   * @return
   */
  public Boolean isEnabled();

  @Deprecated /*this should just be in the attach */
  public void setController(EncoderController controller); // FIXME - is this redundant ????

  /**
   * integer address or pin number
   * 
   * @param address
   */
  public void setPin(Integer address);

  /**
   * for address or pin names - such as D0, D1, A0, .... at some point the
   * controller will "probably" convert this to an actual address
   * 
   * @param address
   */
  public void setPin(String address);

}
