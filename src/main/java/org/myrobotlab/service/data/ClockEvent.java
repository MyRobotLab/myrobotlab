package org.myrobotlab.service.data;

import java.io.Serializable;
import java.util.Date;

public class ClockEvent implements Serializable {

  private static final long serialVersionUID = 1L;
  public Date time = null;
  public String name;
  public String method;
  public Object[] data = null;

  public ClockEvent(Date time, String name, String method, Object[] data) {
    this.time = time;
    this.name = name;
    this.method = method;
    this.data = data;
  }
}