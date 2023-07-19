package org.myrobotlab.service.interfaces;

import java.io.Serializable;

public class ButtonDefinition implements Serializable {

  private static final long serialVersionUID = 1L;
  String axisName;
  Double value;

  public ButtonDefinition(String serviceName, String axisName) {
    this.axisName = axisName;
  }
}
