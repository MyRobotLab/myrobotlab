package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;

  public double volume = 1.0;
  
  public String currentPlaylist = "default";
  
  /**
   * randomly shuffles a play list
   */
  public boolean shuffle = false;
  
  /**
   * repeats a playlist
   */
  public boolean repeat = false;
  
  @Deprecated /* temporal variable */
  public String currentTrack = null;  

  @Deprecated /* use regular "listeners" from ServiceConfig parent */
  public String[] audioListeners;
  
  /**
   * Named map of lists of files
   */
  public Map<String, List<String>> playlists = new TreeMap<>();
    
  /**
   * a multiplier to scale amplitude of output waveform
   */
  public double peakMultiplier = 300.0;
  
  /**
   * sample interval for peak
   */
  public double peakSampleInterval = 2.0;
  
  /**
   * delay to synchronize publishing of peak with actual sound in milliseconds
   */
  public Long peakDelayMs = 10L;
  
  /**
   * resets the peak to 0 after this many milliseconds
   */
  public Long publishPeakResetDelayMs = 100L;
}
