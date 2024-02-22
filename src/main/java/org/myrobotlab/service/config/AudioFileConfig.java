package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;

  public double volume = 1.0;
  
  public String currentPlaylist = "default";
  
  /**
   * Named map of lists of files
   */
  public Map<String, List<String>> playlists = new TreeMap<>();
    
  /**
   * a multiplier to scale amplitude of output waveform
   */
  public double peakMultiplier = 100.0;
  
  /**
   * sample interval for peak
   */
  public double peakSampleInterval = 15;
  
  /**
   * delay to synchronize publishing of peak with actual sound in milliseconds
   */
  public Long peakDelayMs = null;
}
