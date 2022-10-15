package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.IndianTtsConfig;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class IndianTtsMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(IndianTtsMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public IndianTtsMeta() {

    addDescription("Hindi text to speech support - requires keys");
    setCloudService(true);
    addCategory("speech", "cloud");
    setSponsor("moz4r");
    addCategory("speech", "sound");
    addPeer("httpClient", "HttpClient", "httpClient");

    setRequiresKeys(true);

  }

  public Plan getDefault(String name) {

    Plan plan = new Plan(name);
    plan.putPeers(name, peers);

    IndianTtsConfig config = new IndianTtsConfig();
    config.audioFile = name + ".audioFile";

    // add self last - desired order or construction
    plan.addConfig(config);

    return plan;
  }

}
