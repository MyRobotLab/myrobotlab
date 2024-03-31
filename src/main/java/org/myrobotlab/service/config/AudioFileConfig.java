package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;
  public String currentTrack = "default";
  public double volume = 1.0;
  public String currentPlaylist = "default";
  public Map<String, List<String>> playlists;
  
  @Deprecated /* use regular "listeners" from ServiceConfig parent */
  public String[] audioListeners;
  
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
