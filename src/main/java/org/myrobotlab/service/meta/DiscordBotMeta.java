package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DiscordBotMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DiscordBotMeta.class);

  /**
   * DiscordBot metadata.  Uses JDA library
   * JNA versions are excluded to avoid version conflicts with other services.
   * ProgramAB is a peer.
   * 
   * @param name
   *          name of the service
   * 
   */
  public DiscordBotMeta(String name) {
    super(name);
    addDescription("Discord Bot Proxy for chatbot backend.");
    addCategory("chatbot");
    // TODO: define a chat bot as a peer?
    // addPeer("programab", "ProgramAB", "The peer chatbot that is launched with this service.");
    // Also needs an additional repo..
    addDependency("net.dv8tion", "JDA", "4.3.0_277");
    // This conflicts with other services because it has an older version.
    exclude("net.java.dev.jna", "jna");
  }

}
