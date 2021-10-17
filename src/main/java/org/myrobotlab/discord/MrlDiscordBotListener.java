package org.myrobotlab.discord;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.DiscordBot;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.data.Utterance;
import org.slf4j.Logger;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Discord bot listener adapter implementation.
 * This class contains the logic handle a callback when messages
 * are received by the bot.
 * 
 */
public class MrlDiscordBotListener extends ListenerAdapter {

  transient public final static Logger log = LoggerFactory.getLogger(MrlDiscordBotListener.class);
  private final String botName;
  private final ProgramAB brain;
  private final DiscordBot bot;
  public boolean talkToBots = false;

  public MrlDiscordBotListener(DiscordBot bot, ProgramAB brain, String botName) {
    this.bot = bot;
    this.brain = brain;
    this.botName = botName;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    super.onMessageReceived(event);

    // Create an utterance object from the message.
    Utterance utterance = new Utterance();
    utterance.username = event.getAuthor().getName();
    utterance.isBot = event.getAuthor().isBot();
    // TODO: maybe we want the raw content? maybe the displayed content?
    utterance.text = event.getMessage().getContentDisplay();
    // TODO: list of other mentions in the utterance.
    // publish this utterance.  we should see it arrive at programab's onUtterance method.
    bot.invoke("publishUtterance", utterance);

    // TODO: what do  I need to get a handle back to the channel
    MessageChannel channel = event.getChannel();
    //    channel.getId();
    //    channel.getType();
    utterance.channel = channel.getId();
    // TODO: cleanup this message
    System.err.println("channel... " + channel);

    if (event.getAuthor().isBot() && talkToBots) {
      log.info("Not responding to bots.");
      return;
    }
    String userName = event.getAuthor().getName();
    log.info("Message Received from " + userName + " : " + event.getMessage().getContentDisplay());
    // Don't talk to myself!
    if (userName.contentEquals(botName)) {
      // System.out.println("not me..");
      return;
    }
    boolean shouldIRespond = false;
    // always respond to direct messages.
    if (ChannelType.PRIVATE.equals(event.getChannelType())) {
      shouldIRespond = true;
    } else {
      if (!event.getAuthor().isBot()) {
        // TODO: don't talk to bots.. it won't go well..
        List<User> mentioned = event.getMessage().getMentionedUsers();
        for (User u : mentioned) {
          if (u.getName().equals(botName)) { 
            shouldIRespond = true;
            break;
          }
        }
      } 

    }

    // TODO: is there a better way to test for this?
    if (shouldIRespond) {
      log.info("I should respond!");
      // let's respond to the user to their utterance.
      String utteranceDisp = event.getMessage().getContentDisplay();
      // let's strip the @+botname from the beginning of the utterance i guess.
      utteranceDisp = utteranceDisp.replace("@" + botName, "");
      Response resp = respond(userName, utteranceDisp);
      // Ok.. now what? respond to the user ...
      if (!StringUtils.isEmpty(resp.msg) ) {
        event.getChannel().sendMessage(resp.msg).queue();
      } else {
        log.info("No Response from the chatbot brain... now what?");
      }
    }
  }

  private Response respond(String userName, String utterance) {
    log.info("Get Response " + userName + " for: " + utterance);
    Response resp  = brain.getResponse(userName, utterance);
    return resp;    
  }

}
