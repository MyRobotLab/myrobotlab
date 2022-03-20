package org.myrobotlab.service;

import java.util.List;
import java.util.Set;

import javax.security.auth.login.LoginException;

import org.myrobotlab.discord.MrlDiscordBotListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.DiscordBotConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Utterance;
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
public class DiscordBot extends Service implements UtterancePublisher, UtteranceListener {

  transient public final static Logger log = LoggerFactory.getLogger(DiscordBot.class);

  private static final long serialVersionUID = 1L;

  private transient JDA bot;

  protected String botName;

  protected transient MrlDiscordBotListener discordListener;

  protected transient JDABuilder jda = null;

  protected boolean connected = false;

  protected Utterance lastUtterance = null;

  protected String token = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

  public DiscordBot(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  public ServiceConfig apply(ServiceConfig c) {
    DiscordBotConfig config = (DiscordBotConfig) c;

    setToken(config.token);

    if (config.utteranceListeners != null) {
      for (String name : config.utteranceListeners) {
        attachUtteranceListener(name);
      }
    }

    if (config.connect) {
      try {
        connect();
      } catch (Exception e) {
        error("could not connect %s", e.getMessage());
      }
    }

    return config;
  }

  public String getBotName() {
    return botName;
  }

  public void releaseService() {
    super.releaseService();
    disconnect();
  }

  public void attach(Attachable attachable) {
    if (attachable instanceof UtteranceListener) {
      attachUtteranceListener(attachable.getName());
    } else {
      error("don't know how to attach a %s", attachable.getName());
    }
  }

  @Override
  public ServiceConfig getConfig() {
    // TODO: this is also an ugly pattern. you can't really call super get
    // config here!
    // ServiceConfig c = super.getConfig();
    // TODO: is this unsafe?
    // TODO: what sets the type of this config?
    /// TODO: this isn't good OO programming to have to do it this way.
    DiscordBotConfig config = new DiscordBotConfig();
    config.token = token;

    Set<String> listeners = getAttached("publishUtterance");
    config.utteranceListeners = listeners.toArray(new String[listeners.size()]);

    return config;
  }

  public void connect() throws LoginException {
    // TOOD: create a bot and connect with our token
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

  public static void main(String[] args) throws Exception {
    // Brief example of starting a programab chatbot and connecting it to
    // discord
    LoggingFactory.getInstance().setLevel("INFO");
    // Let's create a programab instance.
    ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
    brain.setCurrentBotName("Alice");
    DiscordBot bot = (DiscordBot) Runtime.start("bot", "DiscordBot");

    bot.attachUtteranceListener(brain.getName());
    brain.attachUtteranceListener(bot.getName());
    bot.token = "YOUR_TOKEN_HERE";
    bot.connect();
    // System.err.println("done.. press any key.");
    // System.in.read();
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    // We probably also care about which service produced the utterance?
    // in addition to the channel that it came from.
    // TODO: impl me.
    // Ok.. we need the bot to send a message back to the right channel here.
    // TODO: the idea is if we receive an utterance (from ProgramAB..
    // we should publish it to the proper channel..

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

}
