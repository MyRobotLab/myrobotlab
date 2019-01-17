package org.myrobotlab.programab;

import java.util.Date;

import org.alicebot.ab.Chat;

/**
 * Pojo to wrap non-serializable Chats and provide unique data elements for each
 * Chat session
 * 
 * @author GroG
 *
 */
public class ChatData {

  public boolean processOOB = true;
  public transient Chat chat;
  public Date lastResponseTime = null;
  public boolean enableAutoConversation = false;
  // Number of milliseconds before the robot starts talking on its own.
  public int maxConversationDelay = 5000;

  public ChatData(Chat chat) {
    this.chat = chat;
  }

}
