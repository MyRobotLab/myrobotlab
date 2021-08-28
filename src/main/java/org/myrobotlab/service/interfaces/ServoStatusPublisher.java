package org.myrobotlab.service.interfaces;

public interface ServoStatusPublisher {

  /**
   * Signals the servo has started a move
   * 
   * @param name
   *          the name of the servo started
   * @return the name of the servo started
   * 
   */
  public String publishServoStarted(String name);

  /**
   * Signals the servo has stopped a movement
   * 
   * @param name
   *          of the servo started
   * @return name of the servo started
   * 
   */
  public String publishServoStopped(String name);

}
