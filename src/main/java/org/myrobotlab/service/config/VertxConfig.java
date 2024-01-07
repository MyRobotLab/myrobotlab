package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class VertxConfig extends ServiceConfig {

  public Integer port = 8443;
  public Integer workerCount = 1;
  public boolean ssl = true;
  public boolean autoStartBrowser = true;

  public List<String> root = new ArrayList<>();

  public VertxConfig() {
    root.add("./resource/Vertx/build");
  }

}
