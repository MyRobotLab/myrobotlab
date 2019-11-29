package org.myrobotlab.framework;

public class HelloRequest {
  public String id;
  public String uuid;
  public Platform platform;
  
  public HelloRequest(String id, String uuid) {
    this.id = id;
    this.uuid = uuid;
    this.platform = Platform.getLocalInstance();
  }

  public String toString() {
    return String.format("%s - %s - %s", uuid, id, platform);
  }
}
