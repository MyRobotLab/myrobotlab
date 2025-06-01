package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class VertxConfig extends ServiceConfig {

  /**
   * <pre>
      # legacy
      - resource/WebGui/app
      - resource/InMoov2/peers/WebGui/app
      - resource
      
      # react 
      - src/main/resources/resource/Vertx/build
  
      # debug legacy
      - src/main/resources/resource/WebGui/app
      - src/main/resources/resource/InMoov2/peers/WebGui/app
      - src/main/resources/resource
  
      # debug react port 5000 proxy
   * </pre>
   */

  public Integer port = 8443;
  public boolean ssl = true;
  public boolean autoStartBrowser = true;

  public List<String> root = new ArrayList<>();

  public VertxConfig() {
    // legacy
    root.add("resource/WebGui/app");
    root.add("resource/InMoov2/peers/WebGui/app");
    root.add("resource");
    /**
    <pre>
    // debug legacy
    root.add("src/main/resources/resource/WebGui/app");
    root.add("src/main/resources/resource/InMoov2/peers/WebGui/app");
    root.add("src/main/resources/resource");

    // debug react app
    root.add("resource/Vertx/build");
    </pre>
    */
  }

}
