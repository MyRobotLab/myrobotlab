package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AnalogData;

public interface AnalogListener extends NameProvider {

  /**
   * Configuration related - set the axis for which AxisPublishers will publish
   * to on this service
   * 
   * @param name
   */
  public void setAnalogId(String name);

  /**
   * Configuration related - get the axis for which AxisPublishers will publish
   * to on this service
   */
  public String getAnalogId();

  /**
   * call back method to accept AxisData
   * 
   * @param data
   */
  public void onAnalog(AnalogData data);

  /**
   * Attaches a publisher to this listener
   * 
   * @param publisher
   */
  public void attachAnalogPublisher(AnalogPublisher publisher);

  /**
   * Detaches a publisher to this listener
   * 
   * @param publisher
   */
  public void detachAnalogPublisher(AnalogPublisher publisher);

}
