package org.myrobotlab.service.interfaces;

import java.io.Serializable;

public class ButtonDefinition implements Serializable {

  private static final long serialVersionUID = 1L;
  public String axisName;
  public Double value;
  public String serviceName;

  public ButtonDefinition(String serviceName, String axisName) {
    this.serviceName = serviceName;
    this.axisName = axisName;
  }
}
