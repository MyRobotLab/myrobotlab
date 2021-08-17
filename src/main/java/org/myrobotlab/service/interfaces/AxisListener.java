package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AxisData;

public interface AxisListener extends NameProvider {
  
  /**
   * Configuration related - set the axis for which AxisPublishers will publish to on
   * this service
   *   
   * @param name
   */
  public void setAxisName(String name);

  /**
   * Configuration related - get the axis for which AxisPublishers will publish to on
   * this service
   *   
   * @param name
   */  
  public String getAxisName();
  
  /**
   * call back method to accept AxisData
   * 
   * @param data
   */
  public void onAxis(AxisData data);
}
