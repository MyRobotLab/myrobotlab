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
  
  public Double moveToBlocking(Integer newPos);
  
  public Double moveToBlocking(Float newPos);

  public Double moveToBlocking(Double newPos);
  
  public Double moveToBlocking(Integer newPos, Long timeoutMs);
  
  public Double moveToBlocking(Float newPos, Long timeoutMs);
  
  public Double moveToBlocking(Double newPos, Long timeoutMs);

}
