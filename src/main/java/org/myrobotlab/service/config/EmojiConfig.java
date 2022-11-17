package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;

public class EmojiConfig extends ServiceConfig {

  public String emojiSourceUrlTemplate = "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/{size}/emoji_{code}.png";

  /**
   * http peer to retrieve emojis
   */
  public String http;

  /**
   * display peer to display
   */
  public String display;

  /**
   * map of keys to codes
   */
  public Map<String, String> map = new HashMap<String, String>();

  public int size = 512;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
            
    // default names
    http = name + ".http";
    display = name + ".display";
    
    addPeer(plan, name, "http", http, "Http", "Http");
    addPeer(plan, name, "display", display, "ImageDisplay", "ImageDisplay");
    
    return plan;
  }


}
