package org.myrobotlab.arduino;

import java.io.Serializable;

public class DeviceSummary implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public Integer id;

  public DeviceSummary(String name, int id) {
    this.name = name;
    this.id = id;
  }

  @Override
  public String toString() {
    return String.format("%s device id %d", name, id);
  }

}
