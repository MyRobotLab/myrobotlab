package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.service.Random.Range;

public class RandomConfig extends ServiceConfig {

  public static class RandomMessageConfig {
    public Range[] data;
    public long minIntervalMs;
    public long maxIntervalMs;
  }
  
  public boolean enabled = true;
  public Map<String, RandomMessageConfig> addRandom = new HashMap<>();

}
