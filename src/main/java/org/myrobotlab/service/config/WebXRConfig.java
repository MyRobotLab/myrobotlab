package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.math.MapperSimple;

public class WebXRConfig extends ServiceConfig {
  
  public Map<String, Map<String, Double>> eventMappings = new HashMap<>();
  
  public Map<String, Map<String, MapperSimple>> controllerMappings = new HashMap<>();

  
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    Map<String, MapperSimple> map = new HashMap<>();
    MapperSimple mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.neck", mapper);
    controllerMappings.put("head.orientation.pitch", map);

    map = new HashMap<>();
    mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.rothead", mapper);
    controllerMappings.put("head.orientation.yaw", map);

    map = new HashMap<>();
    mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.roll", mapper);
    controllerMappings.put("head.orientation.roll", map);
    
    return plan;
  }
  
}
