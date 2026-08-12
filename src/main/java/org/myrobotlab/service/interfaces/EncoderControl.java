package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderPublisher;

public interface EncoderControl extends EncoderPublisher, Attachable {

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
   *          the data to publish
   * @return encoder data
   */
  public EncoderData publishEncoderData(EncoderData data);

  /**
   * Attach a controller to an encoder control.
   * 
   * @param controller
   */
  public void attachEncoderController(EncoderController controller);

  /**
   * @return the state of streaming encoder data
   */
  public Boolean isEnabled();

  /**
   * @return the position of the encoder in degrees or cm for linear encoder ?
   */
  public Double getPos();

  /**
   * encoder controls can update their copy of the encoder data and then publish it to listeners.
   * @param data
   */
  public void updateEncoderData(EncoderData data);
  
}