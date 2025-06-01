package org.myrobotlab.service.config;

/**
 * Discord Bot requires only a security access token. This token is created on
 * the discord server and associated with a bot user.
 *
 */
public class DiscordBotConfig extends ServiceConfig {

  public boolean connect = true;
  public String token;
  // REMOVED BECAUSE OVERLAP WITH SUBSCRIPTIONS
  // public String[] utteranceListeners;

}
