package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.math.MapperSimple;

public class WebXrConfig extends ServiceConfig {

  public Integer port = 8888;
  public boolean autoStartBrowser = true;

  /**
   * range and name mappings for orientation and position
   * controller name | servo name | mapping
   */
  public Map<String, Map<String, MapperSimple>> mappings = new HashMap<>();

  public WebXrConfig() {
    
    Map<String, MapperSimple> map = new HashMap<>();
    map.put("i01.head.rollNeck", new MapperSimple(-3.14, 3.14, -90, 270));
    mappings.put("head.orientation.roll", map);

    map = new HashMap<>();
    map.put("i01.head.rothead", new MapperSimple(-3.14, 3.14, -90, 270));
    mappings.put("head.orientation.yaw", map);

    map = new HashMap<>();
    map.put("i01.head.neck", new MapperSimple(-3.14, 3.14, -90, 270));
    mappings.put("head.orientation.pitch", map);

  }
  
}


