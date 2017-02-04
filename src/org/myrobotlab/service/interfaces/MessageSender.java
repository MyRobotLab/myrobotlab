package org.myrobotlab.service.interfaces;

public interface MessageSender {

  public void send(String name, String method);

  public void send(String name, String method, Object... data);

  public Object sendBlocking(String name, String method);

  public Object sendBlocking(String name, String method, Object... data);

  public Object sendBlocking(String name, Integer timeout, String method, Object... data);

}
