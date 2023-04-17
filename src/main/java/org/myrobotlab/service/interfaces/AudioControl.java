package org.myrobotlab.service.interfaces;

public interface AudioControl {

  public void setVolume(double volume);

  public double getVolume();
  
  public void onPlayAudioFile(String file);

  // pause
  // resume
  // interrupt ?
}
