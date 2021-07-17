package org.myrobotlab.programab;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Properties;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.slf4j.Logger;

/**
 * container for all the bot info
 */
public class BotInfo {

  transient public final static Logger log = LoggerFactory.getLogger(BotInfo.class);

  public String name;
  public File path;
  public boolean activated = false;
  private transient Bot bot;
  public Properties properties = new Properties();
  private transient ProgramAB programab;

  /**
   * base64 png
   */
  public String img;

  public BotInfo(ProgramAB programab, File path) {
    this.name = path.getName();
    this.path = path;
    this.programab = programab;
    programab.info("found bot %s", name);
    properties.getProperties(FileIO.gluePaths(path.getAbsolutePath(), "config/properties.txt"));
    log.info("loaded properties");
  }

  /**
   * task to save predicates and getting responses will eventually call getBot
   * we don't want initialization to create 2 when only one is needed
   * 
   * @return the bot object
   */
  public synchronized Bot getBot() {
    if (bot == null) {
      // lazy loading of bot - created on the first use
      if (properties.containsKey("locale")) {
        bot = new Bot(name, path.getAbsolutePath(), java.util.Locale.forLanguageTag(properties.get("locale")));
      } else {
        bot = new Bot(name, path.getAbsolutePath());
      }

      // merge properties - potentially there are 2 sets
      // user can fill the BotInfo with new properties before a Bot is created
      // this now is lazy Bot creation time - so we merge what the user has
      // then set our reference to the same set of properties
      bot.properties.putAll(properties);
      // bot.toJson(String.format("%s.json",name)); - pretty cool to see
      // Mr.Turings brain in json

      // setting reference of BotInfo properties to bot properties
      properties = bot.properties;

      bot.setSraixHandler(new MrlSraixHandler(programab));
    }
    return bot;
  }

  public Bot reload() {
    bot = null;
    return getBot();
  }

  public boolean isActive() {
    return bot != null;
  }

  public void writeAIMLFiles() {
    bot.writeAIMLFiles();
  }

  public void writeQuit() {
    bot.writeQuit();
  }

  public void setProperty(String name2, String value) {
    properties.put(name2, value);
    saveProperties();
  }

  public void saveProperties() {
    Map<String, String> sorted = new TreeMap<>();
    sorted.putAll(properties);
    StringBuilder sb = new StringBuilder();
    for (String key : sorted.keySet()) {
      sb.append(String.format("%s:%s\n", key, sorted.get(key)));
    }
    try {
      FileIO.toFile(FileIO.gluePaths(path.getAbsolutePath(), "config/properties.txt"), sb.toString().getBytes());
    } catch (Exception e) {
      programab.error(e);
    }

  }

  public void removeProperty(String name2) {
    properties.remove(name2);
    saveProperties();
  }

}
