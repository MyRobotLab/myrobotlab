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
    MrlDiscordBotListener discordListener = new MrlDiscordBotListener(brain, botName);
    jda.addEventListeners(discordListener);
    bot = jda.build();
    // bot.get
    // TODO: what now?
  }

  public void attach(ProgramAB brain) {
    // TODO how do we know that we've started...
    // we want to attach our peers properly.
    this.brain = brain;
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
    bot.attach(brain);
    bot.token = "YOUR_TOKEN_HERE"; 
    bot.connect("Mr. Turing");

    System.err.println("done.. press any key.");
    System.in.read();

  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    // We probably also care about which service produced the utterance?
    // in addition to the channel that it came from.
    // TODO: impl me. 
  }

  @Override
  public Utterance publishUtteracnce(Utterance utterance) {
    return utterance;
  }

}
