package org.myrobotlab.service.data;

/**
 * This represents an utterance. It is a represents a message from one user to
 * another.
 * 
 */
public class Utterance {

  /**
   * timestamp of the creation of the message - epoch ms
   */
  public long ts;

  /**
   * unique id of the message
   */
  public String id;

  /**
   * user that produced the utterance
   */
  public String username;

  /**
   * the source of this utterance was a bot or human
   */
  public boolean isBot;

  /**
   * Where the utterance was heard/created. (could be a private/direct message,
   * or it could be to a channel / group)
   */
  public String channel;

  /**
   * PUBLIC / PRIVATE
   */
  public String channelType;

  /**
   * name of channel
   */
  public String channelName;

  /**
   * The text of the utterance
   */
  public String text;

  public String channelBotName;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((channel == null) ? 0 : channel.hashCode());
    result = prime * result + ((channelBotName == null) ? 0 : channelBotName.hashCode());
    result = prime * result + ((channelType == null) ? 0 : channelType.hashCode());
    result = prime * result + (isBot ? 1231 : 1237);
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Utterance other = (Utterance) obj;
    if (channel == null) {
      if (other.channel != null)
        return false;
    } else if (!channel.equals(other.channel))
      return false;
    if (channelBotName == null) {
      if (other.channelBotName != null)
        return false;
    } else if (!channelBotName.equals(other.channelBotName))
      return false;
    if (channelType == null) {
      if (other.channelType != null)
        return false;
    } else if (!channelType.equals(other.channelType))
      return false;
    if (isBot != other.isBot)
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    if (username == null) {
      if (other.username != null)
        return false;
    } else if (!username.equals(other.username))
      return false;
    return true;
  }

  @Override
  public String toString() {
    // TODO: a better tostring.
    return "Utterance [username=" + username + ", isBot=" + isBot + ", channel=" + channel + ", channelType=" + channelType + ", text=" + text + ", channelBotName="
        + channelBotName + "]";
  }

}
