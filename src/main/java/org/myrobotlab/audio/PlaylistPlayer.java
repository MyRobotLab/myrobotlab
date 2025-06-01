package org.myrobotlab.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AudioFile;
import org.slf4j.Logger;

public class PlaylistPlayer implements Runnable {

  static final Logger log = LoggerFactory.getLogger(PlaylistPlayer.class);
  
  private transient AudioFile audioFile = null;
  private transient Thread player;
  private boolean shuffle;
  private boolean repeat;
  private String track;
  private List<String> playlist;
  private boolean done = false;

  public PlaylistPlayer(AudioFile audioFile) {
    this.audioFile = audioFile;
  }

  @Override
  public void run() {    
    while (!done) {

      List<String> list = playlist;
      if (shuffle) {
        list = shuffle(playlist);
      }

      for (int i = 0; i < list.size() && !done; ++i) {
        audioFile.play(list.get(i), true, null, track);
      }
      if (!repeat) {
        log.info("finished playing playlist");
        done = true;
      }
    }
    player = null;
  }

  private List<String> shuffle(List<String> list) {
    List<String> shuffled = new ArrayList<>();
    shuffled.addAll(list);
    Collections.shuffle(shuffled);
    return shuffled;
  }

  public synchronized void stop() {
    done = true;
    audioFile.stop();
    if (player != null) {
      player.interrupt();
    }
  }
  
  public synchronized void skip() {
    if (player != null) {
      audioFile.stop();
    }
  }


  public synchronized void start(List<String> playlist, boolean shuffle, boolean repeat, String track) {
    
    audioFile.getConfig().repeat = repeat;
    audioFile.getConfig().shuffle = shuffle;
    
    if (player != null) {
      audioFile.warn("playlist player already playing a list - stop before starting a new playlist");
      return;
    }

    if (playlist.size() == 0) {
      audioFile.warn("empty playlist");
      return;
    }

    this.playlist = playlist;
    this.shuffle = shuffle;
    this.repeat = repeat;
    this.track = track;
    this.done = false;
    player = new Thread(this, audioFile.getName() + "-playlist-player");
    player.start();
  }

}
