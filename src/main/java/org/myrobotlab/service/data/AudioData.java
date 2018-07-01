package org.myrobotlab.service.data;

import java.io.InputStream;

/**
 * AudioData - represents all of the meta data concerning a sample and the playing of that sample.
 * Items not set (ie null) will be filled by a default in the AudioProcessor.  Items which are
 * set here will always take precedence over the AudioProcessor
 * 
 * @author GroG
 *
 */
public class AudioData {
  
  // FIXME STREAM (InputStream) & AND ENCODING TYPE mp3 wav aiff etc...
  // FIXME USE INPUTSTREAM - filename is just a reference & byte[] can be put in an InputStream 
  
  public transient static final String MODE_BLOCKING = "blocking";
  public transient static final String MODE_QUEUED = "queued";
  
  private transient static long subId = 0; 
  
  /**
   * unique identifier - most significant part will be timestamp - least significant will be
   * atomic(is) nano-precision(ish) static increment
   */
  public long trackId = System.currentTimeMillis() * 1000 + (++subId);
  
  /**
   * mode can be either QUEUED MULTI PRIORITY INTERRUPT OR BLOCKING
   */
  public String mode = AudioData.MODE_QUEUED;
  
  /**
   * String uri reference of audio stream
   * FIXME - nice idea it was uri .. but its not :P
   */
  public String filename = null;
  
  transient public InputStream inputStream = null;
  
  /**
   * repeat : null == never | count == # of times | -1 infinite
   */
  public Integer repeat = null;
  
  /**
   * pause / resume lock 
   */
  public Object waitForLock = null;
  
  /**
   * track for this data to be played on
   * null is default track
   */
  public String track = null;
  
  public Long startTs = null;
  public Long stopTs = null;
  
  // public String state = 

  public Double volume = null; // null == take processor volume | != null == specify own volume
   
  // public float volume = 1.0f; DONE ON TRACK
  // public float balance = 0.0f; SHOULD BE DONE ON TRACK
  // public String track = DEFAULT_TRACK; // default track
  public AudioData(String fileName) {
    this.filename = fileName;
  }
  
  public AudioData(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public String toString() {
    String r = "";
    if (repeat != null){

      if (repeat == -1){
        r = "repeat forever";
      } else {
        r = String.format("repeat %d times", repeat);
      }
    }
    
    return String.format("file : %s  mode : %s trackId : %d %s", filename, mode, trackId, r);
  }

}