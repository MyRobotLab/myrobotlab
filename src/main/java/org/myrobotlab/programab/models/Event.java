package org.myrobotlab.programab.models;

/**
 * Pojo for state change of one of ProgramAB's state info
 * @author GroG
 *
 */
public class Event {
  /**
   * the botName in this state change - typically 
   * current session botName
   */
  public String botname;
  /**
   * unique identifier for the session user and bot
   */
  public String id;
  
  /**
   * name of the predicate changed
   */
  public String name;
    
  /**
   * service this topic change came from
   */
  public String src;
  
  /**
   * new topic or state name in this transition
   */
  public String topic;
   
  /**
   * timestamp
   */
  public long ts = System.currentTimeMillis();
  
  /**
   * the user name in this state change - usually
   * current session userName
   */
  public String user;
  
  /**
   * new value
   */
  public String value;
  
  public Event() {    
  }
  
  public Event(String src, String userName, String botName, String topic) {
    this.src = src;
    this.user = userName;
    this.botname = botName;
    this.topic = topic;
  }
  
  
  @Override
  public String toString() {
    return String.format("%s %s=%s", id, name, value);
  }
}
