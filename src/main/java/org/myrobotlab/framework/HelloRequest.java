package org.myrobotlab.framework;

import java.util.List;

public class HelloRequest {
  public String id;
  public String uuid;
  public Platform platform;
  public List<NameAndType> serviceList;

  public HelloRequest(String id, String uuid, List<NameAndType> serviceList) {
    this.id = id;
    this.uuid = uuid;
    this.platform = Platform.getLocalInstance();
    this.serviceList = serviceList;
  }
  
  public String toString() {
    return String.format("%s - %s - %s", uuid, id, platform);
  }
}
