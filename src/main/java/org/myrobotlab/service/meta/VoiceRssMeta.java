package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VoiceRssMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(VoiceRssMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.VoiceRss");

    Platform platform = Platform.getLocalInstance();
    meta.addDescription("VoiceRss speech synthesis service.");
    meta.addCategory("speech");
    meta.setSponsor("moz4r");
    meta.addCategory("speech", "cloud");
    meta.addTodo("test speak blocking - also what is the return type and AudioFile audio track id ?");
    meta.setCloudService(true);
    meta.addDependency("com.voicerss", "tts", "1.0");
    return meta;
  }

  
}

