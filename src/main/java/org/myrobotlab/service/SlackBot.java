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

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;

/**
 * A slack bot gateway for utterance publishers and listeners.
 * 
 */
public class SlackBot extends Service<SlackBotConfig> implements UtteranceListener, UtterancePublisher {

  public final static Logger log = LoggerFactory.getLogger(SlackBot.class);

  private static final long serialVersionUID = 1L;

  // something like "xoxb-XXXXXXXXX-XXXXXXXX-XXXXXXXXXXXXX"
  String botToken;
  // something like
  // "xapp-X-XXXXXXXX-XXXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
  String appToken;

  public SlackBot(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  @Override
  public SlackBotConfig getConfig() {
    super.getConfig();
    // FIXME remove members and use config only
    config.appToken = appToken;
    config.botToken = botToken;
    return config;
  }

  @Override
  public SlackBotConfig apply(SlackBotConfig c) {
    SlackBotConfig config = (SlackBotConfig) super.apply(c);
    appToken = config.appToken;
    botToken = config.botToken;
    // TODO: should we connect here?
    return config;
  }

  public void connect() throws IOException, Exception {
    // Connect to slack!
    AppConfig appConfig = AppConfig.builder().singleTeamBotToken(botToken).build();
    App app = new App(appConfig);
    // some callbacks..??
    app.event(MessageEvent.class, (req, ctx) -> {
      log.info("Message Event Req: {}  and Ctx: {}", req, ctx);
      // turn this message event into an utterance so we can publish it.
      Utterance utterance = new Utterance();
      utterance.text = req.getEvent().getText();
      utterance.id = req.getEventId();
      utterance.channel = req.getEvent().getChannel();
      utterance.channelType = req.getEvent().getChannelType();
      // TODO: this should be human readable?
      utterance.channelBotName = ctx.getBotUserId();
      utterance.username = req.getEvent().getUser();
      log.info("Utterance: {}", utterance);
      // publish it.
      invoke("publishUtterance", utterance);
      return ctx.ack();
    });

    app.event(AppMentionEvent.class, (req, ctx) -> {
      // We probably don't actually need to register this handler
      // for this event, the ProgramAB instance currently decides
      // if the utterance is intended for the bot...
      log.info("The bot was mentioned in a message.");
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
    log.info("On Utterance: {}", utterance);
    // send the message to the slack channel in the utterance.
    publishMessage(utterance.channel, utterance.text, botToken);
  }

  // helper method to publish a message to a slack channel.
  static void publishMessage(String id, String text, String botToken) {
    // you can get this instance via ctx.client() in a Bolt app
    MethodsClient client = Slack.getInstance().methods();
    try {
      // Call the chat.postMessage method using the built-in WebClient
      ChatPostMessageResponse result = client.chatPostMessage(r -> r
          // The token you used to initialize your app
          .token(botToken).channel(id).text(text)
      // You could also use a blocks[] array to send richer content
      );
      // Print result, which includes information about the message (like TS)
      log.info("result {}", result);
    } catch (IOException | SlackApiException e) {
      log.error("error: {}", e.getMessage(), e);
    }
  }

  public static void main(String[] args) throws IOException, Exception {
    LoggingFactory.getInstance().setLevel("INFO");
    SlackBot slackBot = (SlackBot) Runtime.start("slackBot", "SlackBot");
    // add your app / bot tokens here to authenticate as your bot user.
    String botToken = "xoxb-XXXXXXXX-XXXXXXXXX-XXXXXXXXXXXXXXXXXXXX";
    String appToken = "xapp-X-XXXXXXX-XXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    slackBot.appToken = appToken;
    slackBot.botToken = botToken;
    // Let's see about getting a programAB instance setup and attached to the
    // slack bot
    ProgramAB chatBot = (ProgramAB) Runtime.start("chatBot", "ProgramAB");
    chatBot.setBotType("Mr. Turing");
    slackBot.attachUtteranceListener(chatBot.getName());
    chatBot.attachUtteranceListener(slackBot.getName());
    // Tell the slack bot to connect
    slackBot.connect();
  }
}
