package org.myrobotlab.service.config;

public class InMoov2Config extends ServiceConfig {

  public boolean pirWakeUp = false;

  public boolean pirEnableTracking = false;

  public boolean loadGestures = true;

  public boolean virtual = false;

  public String locale = "en-US";

  /**
   * startup and shutdown will pause inmoov - set the speed to this value then
   * attempt to move to rest
   */
  public double shutdownStartupSpeed = 50;

  public boolean heartbeat = true;

  /**
   * idle time measures the time the fsm is in an idle state
   */
  public boolean idleTimer = true;

}
