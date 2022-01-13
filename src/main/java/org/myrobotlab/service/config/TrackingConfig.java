package org.myrobotlab.service.config;

public class TrackingConfig extends ServiceConfig {

  public String pan;
  public String tilt;
  public String cv;
  public String pid;
  public boolean enabled;
  public long lostTrackingDelayMs = 1000;

}
