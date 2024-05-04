package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.AudioData;

public interface AudioControl {

  public void setVolume(double volume);

  public double getVolume();
  
  /**
   * plays an audiofile - is a listener function for publishAudioFile
   * @param file
   */
  public void onPlayAudioFile(String file);
  
  /**
   * must be a directory, will play one of the audio files within that directory
   * @param dir
   */
  public void onPlayRandomAudioFile(String dir);
  
  /**
   * Plays a random audio file in the given directory
   * @param dir
   */
  public void playRandom(String dir);
  
  /**
   * Plays an audio file
   * @param filename
   * @return
   */
  public AudioData play(String filename);
  
  /**
   * Pause the currently playing audio file
   */
  public void pause();

  /**
   * Resumes the current audio file
   */
  public void resume();
  
  /**
   * stops all audio processors and all tracks
   */
  public void silence();
  
  /**
   * stops the current selected track and audio processor
   */
  public void stop();
}
