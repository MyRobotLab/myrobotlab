package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.EmojiConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class EmojiMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(EmojiMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public EmojiMeta() {
    addDescription("used as a general template");
    addPeer("display", "ImageDisplay", "image display");
    addPeer("http", "HttpClient", "downloader");
    addCategory("general");
  }

  @Override
  public Plan getDefault(String name) {

    Plan plan = new Plan(name);
    plan.putPeers(name, peers);

    EmojiConfig config = new EmojiConfig();
    config.display = name + ".display";
    config.http = name + ".http";

    // add self last - desired order or construction
    plan.addConfig(config);

    return plan;
  }

}
