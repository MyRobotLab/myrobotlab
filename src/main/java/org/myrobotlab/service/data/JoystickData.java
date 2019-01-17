package org.myrobotlab.service.data;

import java.io.Serializable;

/**
 * The value &amp; id of a component. It is sent when the value changes.
 *
 */
public class JoystickData implements Serializable {
  private static final long serialVersionUID = 1L;
  public String id;
  public Float value;

  public JoystickData(String id, Float value) {
    this.id = id;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("[%s] %f", id, value);
  }
}
