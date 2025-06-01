package org.myrobotlab.programab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.alicebot.ab.Bot;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.slf4j.Logger;

/**
 * container for all the bot info It serves as a wrapper for a bot to bundle
 * some additional metadata and helper methods for dealing with a bot.
 */
public class BotInfo {

  transient public final static Logger log = LoggerFactory.getLogger(BotInfo.class);

  public String name;
  public File path;
  public Properties properties = new Properties();
  private transient Bot bot;
  protected org.alicebot.ab.Properties botProperties;
  private transient ProgramAB programab;

  /**
   * base64 png that is an icon or image of what the chatbot looks like.
   */
  public String img;

  public BotInfo(ProgramAB programab, File path) {
    this.name = path.getName();
    this.path = path;
    this.programab = programab;
    programab.info("found bot %s", name);
    try {
      FileInputStream fis = new FileInputStream(FileIO.gluePaths(path.getAbsolutePath(), "manifest.txt"));
      properties.load(new InputStreamReader(fis, Charset.forName("UTF-8")));
      fis.close();
      log.info("loaded properties");
    } catch (FileNotFoundException e) {
      programab.warn("bot %s does not have a manifest.txt", name);
    } catch (Exception e) {
      log.error("BotInfo threw", e);
    }
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
        bot = new Bot(name, path.getAbsolutePath(), java.util.Locale.forLanguageTag((String) properties.get("locale")));
        bot.listener = programab;
      } else {
        if (programab.getLocaleTag() == null) {
          bot = new Bot(name, path.getAbsolutePath());
        } else {
          bot = new Bot(name, path.getAbsolutePath(), java.util.Locale.forLanguageTag(programab.getLocaleTag()));
        }
        bot.listener = programab;
      }

      // merge properties - potentially there are 2 sets
      // user can fill the BotInfo with new properties before a Bot is created
      // this now is lazy Bot creation time - so we merge what the user has
      // then set our reference to the same set of properties
      // bot.properties.putAll(properties);
      // bot.toJson(String.format("%s.json",name)); - pretty cool to see
      // Mr.Turings brain in json

      // setting reference of BotInfo properties to bot properties
      botProperties = bot.properties;

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

  public void setProperty(String name2, String value) {
    properties.put(name2, value);
    saveProperties();
  }

  // FIXME - botProperties should be sent to bot (and verified against aiml tags
  // ?)
  public void saveProperties() {
    Map<String, String> sorted = new TreeMap<>();
    sorted.putAll(botProperties);
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

  @Override
  public String toString() {
    return String.format("%s - %s", name, path);
  }


  public org.alicebot.ab.Properties getBotProperties(){
    return botProperties;
  }
  
}
