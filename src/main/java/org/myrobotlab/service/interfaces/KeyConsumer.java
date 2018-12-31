package org.myrobotlab.service.interfaces;

public interface KeyConsumer {

  public String[] getKeyNames();

  public void setKey(String keyName, String keyValue);

  // only provided by security service
  // public String getKey(String keyName);

}
