package org.myrobotlab.framework.interfaces;

public interface MessageSubscriber {

  public void subscribe(NameProvider topicName, String topicKey);

  public void subscribe(String topicName, String topicKey);

  public void subscribe(String topicName, String topicMethod, String callbackName, String callbackMethod);

  public void unsubscribe(NameProvider topicName, String topicKey);

  public void unsubscribe(String topicName, String topicKey);

  public void unsubscribe(String topicName, String topicMethod, String callbackName, String callbackMethod);

}
