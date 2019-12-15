package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;

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
   * return the state of streaming encoder data
   * 
   * @return
   */
  public Boolean isEnabled();

  /**
   * the position of the encoder in degrees or cm for linear encoder ?
   * @return
   */
  public Double getPos();

}
