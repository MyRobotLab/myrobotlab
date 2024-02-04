package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.service.Random.Range;

public class RandomConfig extends ServiceConfig {

  public static class RandomMessageConfig {
    public Range[] data;
    public long minIntervalMs;
    public long maxIntervalMs;
    public boolean enabled = true;
    public String service;
    public String method;
    
    public RandomMessageConfig() {
    }

    public RandomMessageConfig(String service, String method, long minIntervalMs, long maxIntervalMs, double... ranges) {
      this.service = service;
      this.method = method;
      this.minIntervalMs = minIntervalMs;
      this.maxIntervalMs = maxIntervalMs;
      this.data = new Range[ranges.length / 2];

      for (int i = 0; i < ranges.length; i += 2) {
        Range range = new Range();
        range.min = ranges[i];
        range.max = ranges[i + 1];
        data[i / 2] = range;
      }
    }

    public RandomMessageConfig(String service, String method, long minIntervalMs, long maxIntervalMs, int... ranges) {
      this.service = service;
      this.method = method;
      this.minIntervalMs = minIntervalMs;
      this.maxIntervalMs = maxIntervalMs;
      this.data = new Range[ranges.length / 2];

      for (int i = 0; i < ranges.length; i += 2) {
        Range range = new Range();
        range.min = ranges[i];
        range.max = ranges[i + 1];
        data[i / 2] = range;
      }
    }

    public RandomMessageConfig(String service, String method, long minIntervalMs, long maxIntervalMs, float... ranges) {
      this.service = service;
      this.method = method;
      this.minIntervalMs = minIntervalMs;
      this.maxIntervalMs = maxIntervalMs;
      for (int i = 0; i < (ranges.length / 2); i += 2) {
        Range range = new Range();
        range.min = ranges[i];
        range.max = ranges[i + 1];
        data[i / 2] = range;
      }
    }

  }

  /**
   * enable or disables all random tasks
   */
  public boolean enabled = true;
  
  /**
   * map of random data for messaage creation
   */
  public Map<String, RandomMessageConfig> randomMessages = new HashMap<>();
  /**
   * rate at which each random data enqueued is sampled and evaluated
   */
  public long rate = 100L;

}
