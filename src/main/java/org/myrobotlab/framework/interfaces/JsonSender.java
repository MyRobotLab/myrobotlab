package org.myrobotlab.framework.interfaces;

public interface JsonSender {

  /**
   * Send interface which takes a json encoded Message.
   * For schema look at org.myrobotlab.framework.Message
   * @param jsonEncodedMessage
   */
  public void send(String jsonEncodedMessage);

}
