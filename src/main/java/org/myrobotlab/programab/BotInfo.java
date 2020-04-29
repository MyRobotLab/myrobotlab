package org.myrobotlab.programab;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import org.alicebot.ab.Bot;
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
 
  public BotInfo(ProgramAB programab, File path) {
    StringBuilder sb = new StringBuilder();
    this.name = path.getName();
    this.path = path;
    this.programab = programab;
    sb.append("Found Bot ").append(name);
    try {
      properties.load(new FileInputStream(FileIO.gluePaths(path.getAbsolutePath(), "config/mrl.properties")));
      sb.append(" with config/mrl.properties");
    } catch(Exception e) {}
    sb.append(" @ ").append(path);
    programab.info(sb.toString());
  }

  /**
   * task to save predicates and getting responses will eventually call getBot
   * we don't want initialization to create 2 when only one is needed
   * @return
   */
  public synchronized Bot getBot() {
    if (bot == null) {
      // lazy loading of bot - created on the first use
      if (properties.get("locale") != null) {
        bot = new Bot(name, path.getAbsolutePath(), new Locale((String)properties.get("locale")));
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
