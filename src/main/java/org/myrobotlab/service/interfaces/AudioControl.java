package org.myrobotlab.service.interfaces;

public interface AudioControl {

  // FIXME - should be onVolume(volume)
  public void setVolume(double volume);

  public double getVolume();
  
  /**
   * plays an audiofile - is a listener function for publishAudioFile
   * @param file
   */
  public void onPlayAudioFile(String dir);
  
  /**
   * must be a directory, will play one of the audio files within that directory
   * @param dir
   */
  public void onPlayRandomAudioFile(String dir);

  // onPause
  // onResume
  // onInterrupt ?
}
