package org.myrobotlab.framework;
import org.myrobotlab.codec.CodecUtils;

public class HelloResponse {
  public String id;
  public String uuid;
  public HelloRequest request;
  public Platform platform;
  public Status status;

  // FIXME move serviceList into HelloResponse ... result of introduction ... !
  
  public String toString() {
    return CodecUtils.toJson(this);
  }
}
