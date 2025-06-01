package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AnalogData;

public interface AnalogListener extends NameProvider {

  /**
   * Configuration related - set the axis for which AxisPublishers will publish
   * to on this service
   * 
   * @param name
   *          - sets the name of the access for this AnalogListener e.g. "x"
   */
  public void setAxis(String name);

  /**
   * Configuration related - get the axis for which AxisPublishers will publish
   * to on this service
   */
  public String getAxis();

  /**
   * call back method to accept AxisData
   * 
   * @param data
   *          - the callback analog data
   */
  public void onAnalog(AnalogData data);

  /**
   * Attaches a publisher to this listener
   * 
   * @param publisher
   *          - the publisher to be attached to
   */
  public void attachAnalogPublisher(AnalogPublisher publisher);

  /**
   * Detaches a publisher to this listener
   * 
   * @param publisher
   */
  public void detachAnalogPublisher(AnalogPublisher publisher);

}
