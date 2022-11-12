package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

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

}
