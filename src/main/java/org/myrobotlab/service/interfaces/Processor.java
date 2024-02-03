package org.myrobotlab.service.interfaces;

public interface Processor {

  /**
   * A generalized processing interface currently serving both Python and Py4j.
   * 
   * FIXME - this should be refactored to return an Object
   * but I don't want to break anything for now
   * FIXME - if there is an error this should throw and return
   * and object or null if successful
   * 
   * A processor can exec code
   * @param code
   * @return success or not
   */
  public boolean exec(String code);
  
}
