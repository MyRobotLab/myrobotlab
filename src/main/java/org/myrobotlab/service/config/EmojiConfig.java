package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;

public class EmojiConfig extends ServiceConfig {

  public String emojiSourceUrlTemplate = "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/{size}/emoji_{code}.png";

  /**
   * map of keys to codes
   */
  public Map<String, String> map = new HashMap<String, String>();

  public int size = 512;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    // http peer to retrieve emojis
    addDefaultPeerConfig(plan, name, "http", "HttpClient");
    // peer to display emojis
    addDefaultPeerConfig(plan, name, "display", "ImageDisplay");
    
    return plan;
  }


}
