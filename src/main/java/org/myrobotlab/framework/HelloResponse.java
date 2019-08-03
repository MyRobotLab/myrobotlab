package org.myrobotlab.framework;

// FIXME - send back list of nameTypes ???
public class HelloResponse {
  String id;
  String uuid;
  HelloRequest request;
  Platform platform;

  public HelloResponse(String id, HelloRequest request) {
    this.id = id;
    // this.uuid = uuid;
    this.request = request;
    platform = Platform.getLocalInstance();
  }
}
