package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

// FIXME - make an interface for this - search ? chat ? other ?
public class CleverBot extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(CleverBot.class.getCanonicalName());

  transient ChatterBotFactory factory = null;
  transient ChatterBotSession session = null;
  transient ChatterBot chatterbot = null;
  transient ChatterBotType type = ChatterBotType.PANDORABOTS;
  boolean initialized = false;
  boolean continueToTalkToSelf = true;

  public CleverBot(String n, String id) {
    super(n, id);
    init();
  }

  public String chat(String toSay) {

    try {
      return session.think(toSay);
    } catch (Exception e) {
      log.error("session threw", e);
    }

    return null;
  }

  public boolean init() {
    try {
      factory = new ChatterBotFactory();
      // chatterbot = factory.create(type);
      chatterbot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
      // chatterbot = factory.create(ChatterBotType.CLEVERBOT);
      session = chatterbot.createSession();
    } catch (Exception e) {
      Logging.logError(e);
    }
    return true;
  }

  public String talkToSelf(String input) {
    try {

      ChatterBot bot1 = factory.create(ChatterBotType.CLEVERBOT);
      ChatterBotSession bot1session = bot1.createSession();

      ChatterBot bot2 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
      ChatterBotSession bot2session = bot2.createSession();

      while (continueToTalkToSelf) {

        System.out.println("bot1> " + input);

        input = bot2session.think(input);
        log.info(input);
        System.out.println("bot2> " + input);
        log.info(input);

        input = bot1session.think(input);
      }
    } catch (Exception e) {
      log.error("talkToSelf threw", e);
    }

    return input;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(CleverBot.class);
    meta.addDescription("chatbot service");
    meta.addCategory("ai");
    meta.addDependency("ca.pjer", "chatter-bot-api", "2.0.1");
    meta.addDependency("com.squareup.okhttp3", "okhttp", "3.9.0");
    meta.setCloudService(true);
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init();
    try {
      CleverBot cleverbot = (CleverBot) Runtime.start("cleverbot", "CleverBot");

      log.info(cleverbot.chat("Hi"));

      log.info(cleverbot.chat("How are you?"));
      log.info(cleverbot.chat("what is your name?"));
      log.info(cleverbot.chat("what is 1 + 1?"));
      log.info(cleverbot.chat("why is the sky blue?"));
      log.info(cleverbot.chat("how can i smell?"));
      log.info(cleverbot.chat("who am i?"));
      log.info(cleverbot.chat("tell me a joke"));

      log.info("here");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
