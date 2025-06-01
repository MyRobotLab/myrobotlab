package org.myrobotlab.service.meta;

import org.myrobotlab.service.meta.abstracts.MetaData;

public class SlackBotMeta extends MetaData {
  private static final long serialVersionUID = 1L;

  /**
   * SlackBot Metadata This uses the Slack official API.
   * 
   */
  public SlackBotMeta() {
    super();
    addDescription("Slack Bot Proxy for chatbot backend.");
    addCategory("chatbot");
    addDependency("com.slack.api", "bolt", "1.24.0");
    addDependency("com.slack.api", "bolt-socket-mode", "1.24.0");
    addDependency("javax.websocket", "javax.websocket-api", "1.1");
    addDependency("org.glassfish.tyrus.bundles", "tyrus-standalone-client", "1.19");
  }

}
