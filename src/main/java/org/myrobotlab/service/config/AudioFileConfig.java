package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;
  public String currentTrack = "default";
  public double volume = 1.0;
  public String currentPlaylist = "default";
  public Map<String, List<String>> playlists;
  public double peakMultiplier = 1.0;
  /**
   * sample interval for peak
   */
  public double peakSampleInterval = 30;
}
