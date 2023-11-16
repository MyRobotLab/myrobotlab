package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.service.data.LedDisplayData;

public class NeoPixelConfig extends ServiceConfig {

  public Integer pin = null;
  public Integer pixelCount = null;
  public int pixelDepth = 3;
  public int speed = 10;
  public int red = 0;
  public int green = 0;
  public int blue = 0;
  public String controller = null;
  public String currentAnimation = null;
  public Integer brightness = 255;
  public boolean fill = false;
  // auto clears flashes
  public boolean autoClear = false;
  public int idleTimeout = 1000;
  
  /**
   * Map of predefined led flashes, defined here in configuration.
   * Another service simply needs to publishFlash(name) and the
   * neopixel will get the defined flash data if defined and process 
   * it.
   */
  public Map<String, LedDisplayData> flashMap = new HashMap<>();
  
  
  /**
   * reason why we initialize default for flashMap here, is so 
   * we don't need to do a data copy over to a service's member variable
   */
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
        
    flashMap.put("error", new LedDisplayData(120, 0, 0, 3, 30, 30));
    flashMap.put("info", new LedDisplayData(0, 0, 120, 1, 30, 30));
    flashMap.put("success", new LedDisplayData(0, 0, 120, 2, 30, 30));
    flashMap.put("warn", new LedDisplayData(100, 100, 0, 3, 30, 30));
    flashMap.put("heartbeat", new LedDisplayData(210, 110, 0, 2, 100, 30));
    flashMap.put("pirOn", new LedDisplayData(60, 200, 90, 3, 100, 30));
    flashMap.put("onPeakColor", new LedDisplayData(180, 53, 21, 3, 60, 30));
    flashMap.put("speaking", new LedDisplayData(0, 183, 90, 2, 60, 30));
    
    return plan;
  }


}

