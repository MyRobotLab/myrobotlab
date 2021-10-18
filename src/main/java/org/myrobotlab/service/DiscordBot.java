package org.myrobotlab.service;

import javax.security.auth.login.LoginException;
import org.myrobotlab.discord.MrlDiscordBotListener;
import org.myrobotlab.framework.Service;
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
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Discord Bot to connect to a Discord server / channel 
 * Discord Bots need to be created on the discord server and granted privlidges to join and chat 
 * with channels.  
 * The bot user also needs a token that is used to authenticate and identify the bot. 
 */
public class DiscordBot extends Service implements UtterancePublisher, UtteranceListener {

  transient public final static Logger log = LoggerFactory.getLogger(DiscordBot.class);

  private static final long serialVersionUID = 1L;
  private transient JDA bot;
  public transient ProgramAB brain;

  String token = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

  public DiscordBot(String reservedKey, String inId) {
    super(reservedKey, inId);
  }
  
  @Override
  public ServiceConfig load(ServiceConfig c) {
    DiscordBotConfig config = (DiscordBotConfig)c;
    this.token = config.token;
    return config;
  }

  @Override
  public ServiceConfig getConfig() {
    // TODO: this is also an ugly pattern. you can't really call super get config here!
    // ServiceConfig c =  super.getConfig();
    // TODO: is this unsafe?
    // TODO: what sets the type of this config?
    /// TODO: this isn't good OO programming to have to do it this way.
    DiscordBotConfig config = (DiscordBotConfig) initConfig(new DiscordBotConfig());
    config.token = token;
    return config;
  }

  public void connect(String botName) throws LoginException {
    // TOOD: create a bot and connect with our token
    JDABuilder jda = JDABuilder.createDefault(token);
    MrlDiscordBotListener discordListener = new MrlDiscordBotListener(this);
    jda.addEventListeners(discordListener);
    bot = jda.build();
    // TODO: a hook to properly shutdown the bot i guess?
  }

  /**
   * Disconnect the current bot.
   */
  public void disconnect() {
    // TODO: does this work?  maybe we should do bot.shutdownNow() ?
    // maybe call this in stop service?
    if (bot != null) {
      bot.shutdown();
    }
  }
  
  /**
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * @param token the token to set
   */
  public void setToken(String token) {
    this.token = token;
  }

  public static void main(String[] args) throws Exception {
    // Brief example of starting a programab chatbot and connecting it to discord
    LoggingFactory.getInstance().setLevel("INFO");
    // Let's create a programab instance.
    ProgramAB brain = (ProgramAB)Runtime.start("brain", "ProgramAB");
    brain.setCurrentBotName("Alice");
    DiscordBot bot = (DiscordBot)Runtime.start("bot", "DiscordBot");
    
    bot.attachUtteranceListener(brain.getName());
    brain.attachUtteranceListener(bot.getName());
    
    // bot.attach(brain);
    bot.token = "YOUR_TOKEN_HERE"; 
    bot.connect("Mr. Turing");
    //System.err.println("done.. press any key.");
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
      // TODO: how do i get the channel back so I can respond to it?!  seesh..
      discordChannel.sendMessage(utterance.text).queue();
    } else {
      TextChannel discordChannel = bot.getTextChannelById(utterance.channel);
      discordChannel.sendMessage(utterance.text).queue();
      // TODO: only public channels? or any channel?
    }
  }

  @Override
  public Utterance publishUtterance(Utterance utterance) {
    return utterance;
  }

}
