package org.myrobotlab.framework;

public class QueueStats {

  public String name;
  public int currentQueueCount;
  public int total;
  public int interval;
  public long ts;
  public long lastTS;
  public long delta;
  public long lineSpeed;

  public String getName() {
    return name;
  }

}
