package org.myrobotlab.service.interfaces;

public interface AbsolutePositionControl {

  /**
   * Moves the a specific location. Typically, for example, a servo has 0 to 180
   * positions - each increment corresponding to a degree
   * 
   * @param newPos
   *          the new position to move to.
   * 
   */
  public void moveTo(Integer newPos);
  
  public void moveTo(Float newPos);

  public void moveTo(Double newPos);
  
  public void moveToBlocking(Integer newPos);
  
  public void moveToBlocking(Float newPos);

  public void moveToBlocking(Double newPos);
  
  public void moveToBlocking(Integer newPos, Long timeoutMs);
  
  public void moveToBlocking(Float newPos, Long timeoutMs);
  
  public void moveToBlocking(Double newPos, Long timeoutMs);

}
