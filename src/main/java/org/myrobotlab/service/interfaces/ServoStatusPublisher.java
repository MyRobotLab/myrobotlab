package org.myrobotlab.service.interfaces;

public interface ServoStatusPublisher {

  /**
   * Signals the servo has started a move
   * 
   * @param name
   *          - name of servo
   * @param position
   *          - position where servo started movement
   * @return
   */
  public ServoEvent publishServoStarted(String name, Double position);

  /**
   * Signals the servo has stopped a movement
   * 
   * @param name
   *          - name of servo
   * @param position
   *          - position where servo stopped
   * @return
   */
  public ServoEvent publishServoStopped(String name, Double position);

}
