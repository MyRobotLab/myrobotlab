package org.myrobotlab.service.data;

/**
 * Purpose of this class is to be a simple data POJO
 * for ProgramAB topic changes.  This will be useful to
 * interface the state machine implemented in AIML where
 * topic changes represent changes of state.
 * 
 * @author GroG
 *
 */
public class TopicChange {
 
  /**
   * previous topic or state in this state transition
   */
  public String oldTopic;
  
  /**
   * new topic or state name in this transition
   */
  public String newTopic;
  
  /**
   * the user name in this state change - usually
   * current session userName
   */
  public String userName;
  
  /**
   * the botName in this state change - typically 
   * current session botName
   */
  public String botName;
  
  public TopicChange(String userName, String botName, String newTopic, String oldTopic) {
    this.userName = userName;
    this.botName = botName;
    this.newTopic = newTopic;
    this.oldTopic = oldTopic;
  }
  
  public String toString() {
    return String.format("newTopic: %s oldTopic: %s botName %s userName: %s", newTopic, oldTopic, botName, userName);
  }

}
