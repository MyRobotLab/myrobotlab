package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.service.Random.Range;

public class RandomConfig extends ServiceConfig {

  public static class RandomMessageConfig {
    public Range[] data;
    public long minIntervalMs;
    public long maxIntervalMs;

    public RandomMessageConfig() {
    }

    public RandomMessageConfig(long minIntervalMs, long maxIntervalMs, double... ranges) {
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

    public RandomMessageConfig(long minIntervalMs, long maxIntervalMs, int... ranges) {
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

    public RandomMessageConfig(long minIntervalMs, long maxIntervalMs, float... ranges) {
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

  public boolean enabled = true;
  public Map<String, RandomMessageConfig> randomMessages = new HashMap<>();

}
