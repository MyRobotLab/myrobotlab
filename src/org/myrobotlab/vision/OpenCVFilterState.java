package org.myrobotlab.vision;

public class OpenCVFilterState {
  public String name;
  public String state;
  public Object[] data;

  public OpenCVFilterState(String name, String state, Object... data) {
    this.name = name;
    this.state = state;
    this.data = data;
  }
}
