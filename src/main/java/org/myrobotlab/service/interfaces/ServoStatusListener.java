package org.myrobotlab.service.interfaces;

public interface ServoStatusListener {

  /**
   * Callback for a servo has started
   * 
   * @param name
   */
  public void onServoStarted(String name);

  /**
   * Callback a servo has stopped
   * 
   * @param name
   */
  public void onServoStopped(String name);
  public void onServoStop(ServoControl sc);
  public void onServoEnable(String name);

}
