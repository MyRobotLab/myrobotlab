package org.myrobotlab.framework;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;

public class HelloResponse {
  public String id;
  public String uuid;
  public HelloRequest request;
  public Platform platform;
  public Map<String,String> services;
  
  public String toString() {
    return CodecUtils.toJson(this);
  }
}
