package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DiscordBotMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DiscordBotMeta.class);

  /**
   * DiscordBot metadata. Uses JDA library JNA versions are excluded to avoid
   * version conflicts with other services. ProgramAB is a peer.
   * 
   */
  public DiscordBotMeta() {
    addDescription("Discord Bot Proxy for chatbot backend.");
    addCategory("chatbot");
    addDependency("net.dv8tion", "JDA", "4.3.0_277");
    exclude("net.java.dev.jna", "jna");
  }

}
