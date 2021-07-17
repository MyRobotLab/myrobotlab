package org.myrobotlab.service.interfaces;

public interface ServoStatusPublisher {

  /**
   * Signals the servo has started a move
   * 
   */
  public String publishServoStarted(String name);

  /**
   * Signals the servo has stopped a movement
   * 
   */
  public String publishServoStopped(String name);

}
