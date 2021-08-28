package org.myrobotlab.service.interfaces;

public interface ServoStatusListener {

  /**
   * Callback for a servo has started
   * 
   * @param name
   *          the name of the servo that started.
   */
  public void onServoStarted(String name);

  /**
   * Callback a servo has stopped
   * 
   * @param name
   *          the name of the servo that stopped
   */
  public void onServoStopped(String name);

}
