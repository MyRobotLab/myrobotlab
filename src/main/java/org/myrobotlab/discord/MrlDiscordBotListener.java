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
  private final DiscordBot bot;
  public boolean talkToBots = false;

  public MrlDiscordBotListener(DiscordBot bot) {
    this.bot = bot;
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
    // get the response channel and add it to the utterance 
    MessageChannel channel = event.getChannel();
    utterance.channel = channel.getId();
    utterance.channelType = channel.getType().toString();
    // publish the utterance!
    bot.invoke("publishUtterance", utterance);
  }

}
