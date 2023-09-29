package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class VertXConfig extends ServiceConfig {

  public Integer port = 8443;
  public boolean autoStartBrowser = true;
  public List<String> resources = new ArrayList<>();
  
  public VertXConfig() {
    // robot-x-app build directory 
    // resources.add("./resource/WebGui/app");
  }

}
