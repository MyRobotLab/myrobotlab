package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.SlackBotConfig;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;

/**
 * A slack bot gateway for utterance publishers and listeners.
 * 
 */
public class SlackBot extends Service implements UtteranceListener, UtterancePublisher {

  public final static Logger log = LoggerFactory.getLogger(SlackBot.class);
  
  private static final long serialVersionUID = 1L;

  // something like "xoxb-XXXXXXXXX-XXXXXXXX-XXXXXXXXXXXXX" 
  String botToken;
  // something like "xapp-X-XXXXXXXX-XXXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" 
  String appToken;
  
  public SlackBot(String reservedKey, String inId) {
    super(reservedKey, inId);
  }
  
  @Override
  public ServiceConfig getConfig() {
    SlackBotConfig config = new SlackBotConfig();
    config.appToken = appToken;
    config.botToken = botToken;
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    SlackBotConfig config = (SlackBotConfig) c;
    appToken = config.appToken;
    botToken = config.botToken;
    // TODO: should we connect here?
    return config;
  }
  
  public void connect() throws IOException, Exception {
    // Connect to slack!
    AppConfig appConfig = AppConfig.builder()
        .singleTeamBotToken(botToken)
        .build();
    App app = new App(appConfig);
    // some callbacks..??
    app.event(MessageEvent.class, (req, ctx) -> {
      log.info("Ok... here we go!");
      log.info("REQ: {}", req);
      log.info("CTX: {}", ctx);
      // TODO: publish the utterance
      Utterance utterance = new Utterance();
      utterance.text = req.getEvent().getText();
      utterance.id = req.getEventId();
      utterance.channel = req.getEvent().getChannel();
      utterance.channelType = req.getEvent().getChannelType();
      // TODO: this should be human readable?
      utterance.channelBotName = ctx.getBotUserId();
      utterance.username = req.getEvent().getUser();
      log.info("Utterance: {}",utterance);
      invoke("publishUtterance", utterance);
      return ctx.ack();
    });

    app.event(AppMentionEvent.class, (req, ctx) -> {
      log.info("MENTION EVENT?");
      ctx.say("Hi there!");
      return ctx.ack();
    });

    SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
    // This does not block the current thread
    socketModeApp.startAsync();
  }

  @Override
  public Utterance publishUtterance(Utterance utterance) {
    // publish the utterance to the listerners.
    return utterance;
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {
    // TODO: if ProgramAB or other utterance publisher sends us an utterance 
    // we need to relay the message to the proper slack channel
  }

  public static void main(String[] args) throws IOException, Exception {
    LoggingFactory.getInstance().setLevel("INFO");
    //  
    SlackBot slackBot = (SlackBot)Runtime.start("slackBot", "SlackBot");
    // TODO add your app / bot tokens here to authenticate as your bot user.
    slackBot.appToken = "XXXX";
    slackBot.botToken = "YYYY";
    slackBot.connect();
    // TODO: now what?

  }

}
