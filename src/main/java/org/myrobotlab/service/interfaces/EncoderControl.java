package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;

public interface EncoderControl extends Attachable {

  /**
   * stop the stream of encoder data
   */
  public void disable();

  /**
   * enable this encoder to provide a stream of encoder data
   */
  public void enable();

  /**
   * publishes the EncoderData from the encoder
   * 
   * @param data
   * @return data
   */
  public EncoderData publishEncoderData(EncoderData data);

  /**
   * attaches an encoder listener to get the publishEncoderData
   * to invoke the onEncoderData method of the listener.
   * 
   * @param listener
   */
  public void attachEncoderListener(EncoderListener listener);
  
  /**
   * return the state of streaming encoder data
   * 
   * @return
   */
  public Boolean isEnabled();

  /**
   * the position of the encoder in degrees or cm for linear encoder ?
   * 
   * @return
   */
  public Double getPos();

}
