package org.myrobotlab.service.config;

public class VertxConfig extends ServiceConfig {

  public Integer port = 8443;
  public Integer workerCount = 1;
  public boolean ssl = true;
  public boolean autoStartBrowser = true;

  public String root = "./resource/Vertx/build";

}
