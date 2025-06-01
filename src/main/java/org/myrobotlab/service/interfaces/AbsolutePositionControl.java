package org.myrobotlab.service.interfaces;

public interface AbsolutePositionControl {

  public Double moveTo(Integer newPos);

  /**
   * Moves the a specific location. Typically, for example, a servo has 0 to 180
   * positions - each increment corresponding to a degree
   * 
   * @param newPos
   *          the new position to move to.
   * @return TODO
   * 
   */
  public Double moveTo(Double newPos);

  public Double moveToBlocking(Integer newPos);

  public Double moveToBlocking(Double newPos);

  public Double moveToBlocking(Integer newPos, Long timeoutMs);

  public Double moveToBlocking(Double newPos, Long timeoutMs);

}
