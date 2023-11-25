package org.myrobotlab.service.data;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.service.config.NeoPixelConfig.Flash;

/**
 * This class is a composite of possible led display details.
 * Flashes, animations, etc.
 * 
 * @author GroG
 *
 */
public class LedDisplayData {

  /**
   * required action field may be
   *  fill | flash | play animation | stop | clear
   */
  public String action; 
  
  /**
   * name of animation
   */
  public String animation = null;

  /**
   * flash definition
   */
  public List<Flash> flashes = new ArrayList<>();
  
  /**
   * if set overrides default brightness
   */  
  public Integer brightness = null;

  /**
   * begin fill address
   */
  public int beginAddress;

  /**
   * fill count
   */
  public int onCount; 


  public LedDisplayData(String action) {
    this.action = action;
  }

  public String toString() {
    return String.format("%s, %s", action, animation);
  }
}
