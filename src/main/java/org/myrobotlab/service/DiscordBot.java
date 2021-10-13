package org.myrobotlab.service;

import javax.security.auth.login.LoginException;
import org.myrobotlab.discord.MrlDiscordBotListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.DiscordBotConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

/**
 * Discord Bot to connect to a Discord server / channel 
 * Discord Bots need to be created on the discord server and granted privlidges to join and chat 
 * with channels.  
 * The bot user also needs a token that is used to authenticate and identify the bot. 
 */
public class DiscordBot extends Service {

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
    super.load(c);
    DiscordBotConfig config = (DiscordBotConfig)c;
    this.token = config.token;
    return config;
  }

  @Override
  public ServiceConfig getConfig() {
    ServiceConfig c =  super.getConfig();
    // TODO: is this unsafe?
    DiscordBotConfig config = (DiscordBotConfig)c;
    config.token = token;
    return config;
  }

  public void connect() throws LoginException {
    // TOOD: create a bot and connect with our token
    JDABuilder jda = JDABuilder.createDefault(token);
    MrlDiscordBotListener discordListener = new MrlDiscordBotListener(brain);
    jda.addEventListeners(discordListener);
    bot = jda.build();
    // TODO: what now?
  }

  private void setBrain(ProgramAB brain) {
    // TODO how do we know that we've started...
    // we want to attach our peers properly.
    this.brain = brain;
  }

  public static void main(String[] args) throws Exception {

    // Brief example of starting a programab chatbot and connecting it to discord
    LoggingFactory.getInstance().setLevel("INFO");
    // Let's create a programab instance.
    ProgramAB brain = (ProgramAB)Runtime.start("brain", "ProgramAB");
    brain.setCurrentBotName("Alice");

    DiscordBot bot = (DiscordBot)Runtime.start("bot", "DiscordBot");
    bot.setBrain(brain);
    bot.connect();

    System.err.println("done.. press any key.");
    System.in.read();

  }

}
