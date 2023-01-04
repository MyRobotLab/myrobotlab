package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class RosConfig extends ServiceConfig {

  public boolean connect = false;
  public long serviceCallTimeoutMs = 3000;
  public String bridgeUrl = "ws://localhost:9090";
  public List<String> subscriptions = new ArrayList<>();

}
