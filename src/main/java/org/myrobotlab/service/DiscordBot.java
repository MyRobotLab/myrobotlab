package org.myrobotlab.service;

import java.util.List;
import java.util.Set;

import org.myrobotlab.discord.MrlDiscordBotListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.DiscordBotConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Discord Bot to connect to a Discord server / channel Discord Bots need to be
 * created on the discord server and granted privileges to join and chat with
 * channels. The bot user also needs a token that is used to authenticate and
 * identify the bot.
 */
public class DiscordBot extends Service<DiscordBotConfig> implements UtterancePublisher, UtteranceListener, ImageListener {

  transient public final static Logger log = LoggerFactory.getLogger(DiscordBot.class);

  private static final long serialVersionUID = 1L;

  private transient JDA bot;

  protected String botName;

  protected transient MrlDiscordBotListener discordListener;

  protected transient JDABuilder jda = null;

  protected boolean connected = false;

  protected Utterance lastUtterance = null;

  protected String token = null;

  public DiscordBot(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  @Override
  public DiscordBotConfig apply(DiscordBotConfig c) {
    super.apply(c);

    if (config.token != null) {
      setToken(config.token);
    }

    // REMOVED - OVERLAP WITH SUBSCRIPTIONS
//    if (config.utteranceListeners != null) {
//      for (String name : config.utteranceListeners) {
//        attachUtteranceListener(name);
//      }
//    }

    if (config.connect && config.token != null && !config.token.isEmpty()) {
      connect();
    } else if (config.token == null || config.token.isEmpty()) {
      error("requires valid token to connect");
    }

    return config;
  }

  public String getBotName() {
    return botName;
  }

  @Override
  public void releaseService() {
    super.releaseService();
    disconnect();
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof UtteranceListener) {
      attachUtteranceListener(attachable.getName());
    }

    if (attachable instanceof ImagePublisher) {
      attachImagePublisher(attachable.getName());
    }

    if (!(attachable instanceof UtteranceListener) && !(attachable instanceof ImagePublisher)) {
      error("don't know how to attach a %s", attachable.getName());
    }
  }

  @Override
  public DiscordBotConfig getConfig() {
    super.getConfig();
    config.token = token;
    return config;
  }

  public void connect() {
    try {
      if (bot == null) {
        jda = JDABuilder.createDefault(token);
        discordListener = new MrlDiscordBotListener(this);
        jda.addEventListeners(discordListener);
        bot = jda.build();
        botName = bot.getSelfUser().getName();
        connected = true;
        broadcastState();
      } else {
        info("discord bot %s already connected", botName);
      }
    } catch (Exception e) {
      error(e.getMessage());
    }

  }

  /**
   * Disconnect the current bot.
   */
  public void disconnect() {
    // TODO: does this work? maybe we should do bot.shutdownNow() ?
    // maybe call this in stop service?
    if (bot != null) {
      jda.removeEventListeners(discordListener);
      bot.shutdown();
      bot = null;
    }
    connected = false;
  }

  /**
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * @param token
   *          the token to set
   */
  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    // We probably also care about which service produced the utterance?
    // in addition to the channel that it came from.
    // TODO: impl me.
    // Ok.. we need the bot to send a message back to the right channel here.
    // TODO: the idea is if we receive an utterance (from ProgramAB..
    // we should publish it to the proper channel..

    if (utterance == null || utterance.channel == null) {
      error("cannot send utterance channel id unknown");
      return;
    }

    if (utterance.text == null || utterance.text.strip().length() == 0) {
      log.info("no response");
      return;
    }

    if ("PRIVATE".equals(utterance.channelType)) {
      // Private message channel.
      PrivateChannel discordChannel = bot.getPrivateChannelById(utterance.channel);
      // TODO: assume that I should this?
      // TODO: how do i get the channel back so I can respond to it?! seesh..
      discordChannel.sendMessage(utterance.text).queue();
    } else {
      TextChannel discordChannel = bot.getTextChannelById(utterance.channel);
      discordChannel.sendMessage(utterance.text).queue();
      // TODO: only public channels? or any channel?
    }
  }

  /**
   * Sends text on behalf of the bot to the general channel
   * 
   * @param text
   */
  public void sendUtterance(String text) {
    sendUtterance(text, null);
  }

  /**
   * Injects or sends text
   * 
   * @param text
   * @param channelName
   */
  public void sendUtterance(String text, String channelName) {
    if (channelName == null) {
      channelName = "general";
    }
    List<TextChannel> channels = bot.getTextChannelsByName(channelName, true);
    for (TextChannel channel : channels) {
      channel.sendMessage(text).queue();
    }
  }

  public void sendReaction(String code) {
    sendReaction(code, null, null);
  }

  /**
   * sends a reaction to the a previous message id
   * 
   * @param code
   * @param channelName
   */
  public void sendReaction(String code, String id, String channelName) {

    if (code == null || code.trim().isEmpty()) {
      error("no code value");
      return;
    }

    code = code.trim();

    if (channelName == null) {
      channelName = "general";
    }

    if (id == null && lastUtterance != null) {
      id = lastUtterance.id;
    }

    // TODO - dunno how to do this without a message id

    List<TextChannel> channels = bot.getTextChannelsByName(channelName, true);
    for (TextChannel channel : channels) {
      channel.addReactionById(id, code).queue();
    }
  }

  /**
   * this publishing point is omni-directional in that all utterances regardless
   * of direction or channel are published here
   */
  @Override
  public Utterance publishUtterance(Utterance utterance) {
    lastUtterance = utterance;
    return utterance;
  }

  public void setBotName(String name) {
    this.botName = name;
  }

  @Override
  public void onImage(ImageData img) {
    sendImage(img, null, null);
  }

  public void sendImage(ImageData img, String id, String channelName) {
    if (channelName == null) {
      channelName = "general";
    }

    if (id == null && lastUtterance != null) {
      id = lastUtterance.id;
    }

    if (img.src != null && img.src.startsWith("http")) {
      sendUtterance(img.src, channelName);
    } else {
      // TODO - implement binary message
      log.error("implement binary message");
    }

  }

  public static void main(String[] args) throws Exception {
    try {

      // Brief example of starting a programab chatbot and connecting it to
      // discord
      LoggingFactory.getInstance().setLevel("INFO");

      Runtime.startConfig("mrturing");

      DiscordBot bot = (DiscordBot) Runtime.start("bot", "DiscordBot");
      bot.attach("brain.search");
      bot.attach("brain");

      // Runtime.start("webgui", "WebGui");
      // Runtime.start("brain", "ProgramAB");
      // DiscordBot bot = (DiscordBot) Runtime.start("bot", "DiscordBot");
      // bot.setToken("XXXXXXXXXXXXXXXXXXXXXX");
      // bot.attach("brain.search");
      // bot.attach("brain");
      // bot.connect();
      // Runtime.saveConfig("mrturing");

      // bot.attach("brain.search");

      // Runtime.setConfig("mrturing");

      // // Let's create a programab instance.
      // ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
      // brain.setCurrentBotName("Alice");
      // DiscordBot bot = (DiscordBot) Runtime.start("bot", "DiscordBot");
      //
      // bot.runMrT();

      // Runtime.load("mrturing");

      boolean done = true;
      if (done) {
        return;
      }

      // bot.attachUtteranceListener(brain.getName());
      // brain.attachUtteranceListener(bot.getName());
      // bot.id =
      // bot.token = "YOUR_TOKEN_HERE";
      // bot.connect();
      // System.err.println("done.. press any key.");
      // System.in.read();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}