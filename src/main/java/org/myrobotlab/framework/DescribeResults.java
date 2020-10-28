package org.myrobotlab.framework;

import org.myrobotlab.codec.CodecUtils;

public class AuthResponse {
  public String id;
  public String uuid;
  public HelloRequest request;
  public Platform platform;
  public Status status;

  // FIXME move serviceList into authenticate ... result of introduction ... !

  public String toString() {
    return CodecUtils.toJson(this);
  }
}
