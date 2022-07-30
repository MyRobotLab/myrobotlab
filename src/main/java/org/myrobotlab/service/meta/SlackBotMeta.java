package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SlackBotMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SlackBotMeta.class);

  /**
   * SlackBot Metadata
   * This uses the Slack official API.
   * 
   */
  public SlackBotMeta() {
    super();
    addDescription("Slack Bot Proxy for chatbot backend.");
    addCategory("chatbot");
    addDependency("com.slack.api", "bolt", "1.12.1");
    addDependency("com.slack.api", "bolt-socket-mode", "1.12.1");
    addDependency("javax.websocket", "javax.websocket-api", "1.1");
    addDependency("org.glassfish.tyrus.bundles", "tyrus-standalone-client", "1.17");
    // <!--  Slack stuff -->
    // <dependency>
    //   <groupId>com.slack.api</groupId>
    //   <artifactId>bolt</artifactId>
    //   <version>1.12.1</version>
    // </dependency>
    // <dependency>
    //   <groupId>com.slack.api</groupId>
    //   <artifactId>bolt-socket-mode</artifactId>
    //   <version>1.12.1</version>
    // </dependency>
    // <dependency>
    //   <groupId>javax.websocket</groupId>
    //   <artifactId>javax.websocket-api</artifactId>
    //   <version>1.1</version>
    // </dependency>
    // <dependency>
    //   <groupId>org.glassfish.tyrus.bundles</groupId>
    //   <artifactId>tyrus-standalone-client</artifactId>
    //   <version>1.17</version>
    // </dependency>
  }

}
