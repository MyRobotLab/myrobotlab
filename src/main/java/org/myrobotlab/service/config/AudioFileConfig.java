package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;

public class AudioFileConfig extends ServiceConfig {

  public boolean mute = false;
  public String currentTrack;
  public double volume = 1.0;
  public String currentPlaylist;
  public Map<String, List<String>> playlists;

}
