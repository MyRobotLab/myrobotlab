package org.myrobotlab.framework.interfaces;

public interface MessageSender extends NameProvider {

  public void send(String name, String method);

  public void send(String name, String method, Object... data);

  public Object sendBlocking(String name, String method);

  public Object sendBlocking(String name, String method, Object... data);

  public Object sendBlocking(String name, Integer timeout, String method, Object... data);

}
