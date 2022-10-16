package org.myrobotlab.discord;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.DiscordBot;
import org.myrobotlab.service.data.Utterance;
import org.slf4j.Logger;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Discord bot listener adapter implementation. This class contains the logic
 * handle a callback when messages are received by the bot.
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
    utterance.ts = System.currentTimeMillis();
    utterance.id = event.getMessage().getId();

    // Author of the message.
    utterance.username = event.getAuthor().getName();
    utterance.isBot = event.getAuthor().isBot();
    // TODO: maybe we want the raw content? maybe the displayed content?
    // TODO: replace the bot name if it occurs at the beginning of the
    // utterance.
    utterance.text = event.getMessage().getContentDisplay();
    // TODO: list of other mentions in the utterance.
    // get the response channel and add it to the utterance
    MessageChannel channel = event.getChannel();
    utterance.channel = channel.getId();
    utterance.channelName = channel.getName();
    utterance.channelType = channel.getType().toString();
    // copy the name of the bot in the channel that is being used to
    // communicate.
    utterance.channelBotName = bot.getBotName();
    // publish the utterance!
    // TODO: don't publish the message if it came from the bots own self!
    bot.invoke("publishUtterance", utterance);
  }

}
