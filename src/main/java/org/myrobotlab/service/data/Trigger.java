package org.myrobotlab.service.data;

import java.io.Serializable;

public class Trigger implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int BOUNDRY = 1;
  public static final int STATE_LOW = 2;
  public static final int STATE_HIGH = 3;

  public String name;
  public int min;
  public int max;
  public int type; // ONESHOT (only) | MEAN ? EDGE TRIGGER | EDGE DELAY |FIXME
  // - ENUMS
  public int delay;
  public Pin pinData = null;// = new PinData();
  public int targetPin;
  public int threshold; // use this

  public Trigger() {
  }

  public Trigger(String n, int min, int max, int type, int delay, int targetPin) {
    this.name = n;
    this.min = min;
    this.max = max;
    this.type = type;
    this.delay = delay;
    this.targetPin = targetPin;
  }

}
