package org.myrobotlab.framework;

public class HelloRequest {
  
  /**
   * id of the foreign process
   */
  public String id;
  /**
   * connection uuid
   */
  public String uuid;
  
  /**
   * platform of the foreign process
   */
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
