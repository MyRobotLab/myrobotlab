package org.myrobotlab.service.data;

import org.myrobotlab.service.AudioFile;

public class AudioData {
  /**
   * mode can be either QUEUED MULTI PRIORITY INTERRUPT OR BLOCKING
   */
  public String mode = AudioFile.MODE_QUEUED;
  public String file = null;
  public long trackId = System.currentTimeMillis();

  // public float volume = 1.0f; DONE ON TRACK
  // public float balance = 0.0f; SHOULD BE DONE ON TRACK
  // public String track = DEFAULT_TRACK; // default track
  public AudioData(String fileName) {
    this.file = fileName;
  }

  public String toString() {
    return String.format("file : %s  mode : %s trackId : %d", file, mode, trackId);
  }
}