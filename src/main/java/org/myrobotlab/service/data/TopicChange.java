package org.myrobotlab.service.data;

public class TopicChange {
 
  public String oldTopic;
  
  public String newTopic;
  
  public String userName;
  
  public String botName;
  
  public TopicChange(String userName, String botName, String newTopic, String oldTopic) {
    this.userName = userName;
    this.botName = botName;
    this.newTopic = newTopic;
    this.oldTopic = oldTopic;
  }
  

}
