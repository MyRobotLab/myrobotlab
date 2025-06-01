package org.myrobotlab.programab;

/**
 * Pojo for state change of one of ProgramAB's state info
 * @author GroG
 *
 */
public class PredicateEvent {
  /**
   * unique identifier for the session user and bot
   */
  public String id;
  /**
   * name of the predicate changed
   */
  public String name;
  
  // public String previousValue;
  
  /**
   * new value
   */
  public String value;
  public String botName;
  public String userName;
  
  @Override
  public String toString() {
    return String.format("%s %s=%s", id, name, value);
  }
}
