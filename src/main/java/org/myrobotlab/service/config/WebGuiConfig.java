package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class WebGuiConfig extends ServiceConfig {

  public Integer port = 8888;
  public boolean autoStartBrowser = true;
  public boolean enableMdns = false;
  public List<String> resources = new ArrayList<>();
  
  public WebGuiConfig() {
    resources.add("./resource/WebGui/app");
    resources.add("./resource");    
  }

}
