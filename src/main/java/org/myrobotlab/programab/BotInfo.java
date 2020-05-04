package org.myrobotlab.programab;

import java.io.File;
import java.io.FileInputStream;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Properties;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.slf4j.Logger;
import org.myrobotlab.service.data.Locale;
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
  // public Locale locale = new Locale("en-US");
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
   * @return
   */
  public synchronized Bot getBot() {
    if (bot == null) {
      // lazy loading of bot - created on the first use
      
      if (properties.containsKey("locale")) {
        bot = new Bot(name, path.getAbsolutePath(), java.util.Locale.forLanguageTag(properties.get("locale")));
      } else {
        bot = new Bot(name, path.getAbsolutePath());
      }

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

  
}
