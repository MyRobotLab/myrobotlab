package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.math.MapperSimple;

public class WebXRConfig extends ServiceConfig {

  /**
   * Mapping to handle differences in rotation or position of data from
   * sensor
   */
  public Map<String, Map<String, MapperSimple>>  mappings = new HashMap<>();

}
