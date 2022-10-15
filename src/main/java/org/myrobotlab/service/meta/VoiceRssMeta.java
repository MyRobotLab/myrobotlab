package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.VoiceRssConfig;
import org.myrobotlab.service.meta.abstracts.AbstractSpeechSynthesisMeta;
import org.slf4j.Logger;

public class VoiceRssMeta extends AbstractSpeechSynthesisMeta {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VoiceRssMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public VoiceRssMeta() {
    addDescription("VoiceRss speech synthesis service.");
    addCategory("speech");
    setSponsor("moz4r");
    addCategory("speech", "cloud");
    addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    setCloudService(true);
    addDependency("com.voicerss", "tts", "1.0");
  }

  public Plan getDefault(String name) {

    Plan plan = new Plan(name);
    plan.putPeers(name, peers);

    VoiceRssConfig config = new VoiceRssConfig();
    config.audioFile = name + ".audioFile";

    // add self last - desired order or construction
    plan.addConfig(config);

    return plan;
  }

}
