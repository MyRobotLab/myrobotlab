package org.myrobotlab.service.interfaces;

public interface TextPublisher {

  public String getName();

  public String publishText(String text);

  public void addTextListener(TextListener service);

}
