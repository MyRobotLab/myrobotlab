package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;

public class NeoPixelConfig extends ServiceConfig {

  /**
   * when attached to an audio file service the animation to be
   * played when audio is playing
   */
  public String audioAnimation = "Ironman";

  /**
   * pin number of controller
   */
  public Integer pin = null;

  /**
   * Number or pixes for this neo pixel ranges from 8 to 256+
   */
  public Integer pixelCount = null;

  /**
   * color depth 3 RGB or 4 (with white)
   */
  public int pixelDepth = 3;

  /**
   * default speed (fps) of animations
   */
  public int speed = 10;
  /**
   * default red color component
   */
  public int red = 0;
  /**
   * default green color component
   */
  public int green = 0;
  /**
   * default blue color component
   */
  public int blue = 0;
  /**
   * the neopixel controller
   */
  public String controller = null;
  /**
   * the current animation
   */
  public String currentAnimation = null;
  /**
   * initial brightness
   */
  public Integer brightness = 255;
  /**
   * initial fill
   */
  public boolean fill = false;<<<<<<<HEAD=======

  >>>>>>>bc934273b87f95e8adf339057f7aa7a80ed43c91

  /**
   * Map of predefined led flashes, defined here in configuration. Another
   * service simply needs to publishFlash(name) and the neopixel will get the
   * defined flash data if defined and process it.
   */
  public Map<String, Flash[]> flashMap = new HashMap<>();

  public static class Flash {

    /**
     * uses color specify unless null uses default
     */
    public int red = 0;

    /**
     * uses color specify unless null uses default
     */
    public int green = 0;
    /**
     * uses color specify unless null uses default
     */
    public int blue = 0;
    /**
     * uses color specify unless null uses default
     */
    public int white = 0;

    /**
     * time this flash remains on
     */
    public long timeOn = 500;

    /**
     * time this flash remains off
     */
    public long timeOff = 500;

    public Flash() {
    }

    public Flash(int red, int green, int blue, long timeOn, long timeOff) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.timeOn = timeOn;
      this.timeOff = timeOff;
    }

  }

  /**
   * reason why we initialize default for flashMap here, is so we don't need to
   * do a data copy over to a service's member variable
   */
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    flashMap.put("error",
        new Flash[] { new Flash(120, 0, 0, 30, 30), new Flash(120, 0, 0, 30, 30), new Flash(120, 0, 0, 30, 30) });
    flashMap.put("info", new Flash[] { new Flash(120, 0, 0, 30, 30) });
    flashMap.put("success", new Flash[] { new Flash(0, 0, 120, 30, 30) });
    flashMap.put("warn",
        new Flash[] { new Flash(100, 100, 0, 30, 30), new Flash(100, 100, 0, 30, 30), new Flash(100, 100, 0, 30, 30) });
    flashMap.put("heartbeat", new Flash[] { new Flash(210, 110, 0, 100, 30), new Flash(210, 110, 0, 100, 30) });
    flashMap.put("pir",
        new Flash[] { new Flash(60, 200, 90, 30, 30), new Flash(60, 200, 90, 30, 30), new Flash(60, 200, 90, 30, 30) });
    flashMap.put("speaking", new Flash[] { new Flash(0, 183, 90, 60, 30), new Flash(0, 183, 90, 60, 30) });

    return plan;
  }

}
