package org.myrobotlab.service.interfaces;

public interface ServoStatusPublisher {

  /**
   * Signals the servo has started a move
   * 
   * @param name
   * @return
   */
  public String publishServoStarted(String name);

  /**
   * Signals the servo has stopped a movement
   * 
   * @param name
   * @return
   */
  public String publishServoStopped(String name);

}
