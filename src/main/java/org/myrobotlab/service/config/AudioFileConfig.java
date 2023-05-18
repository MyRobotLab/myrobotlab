package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;
  public String currentTrack = "default";
  public double volume = 1.0;
  public String currentPlaylist = "default";
  public Map<String, List<String>> playlists;
  public double peakMultiplier = 100.0;
  public String[] audioListeners;

  /**
   * sample interval for peak
   */
  public double peakSampleInterval = 15;
  /**
   * delay to synchronize publishing of peak with actual sound in milliseconds
   */
  public Long peakDelayMs = null;
}
