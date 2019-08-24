package org.myrobotlab.framework;
import java.util.Map;

// FIXME - send back list of nameTypes ???
public class HelloResponse {
  public String id;
  public String uuid;
  public HelloRequest request;
  public Platform platform;
  public Map<String,String> services;
}
