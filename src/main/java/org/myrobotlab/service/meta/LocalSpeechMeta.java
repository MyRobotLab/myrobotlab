package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class LocalSpeechMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(LocalSpeechMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.LocalSpeech");
    meta.addCategory("speech", "sound");
    meta.addDescription("Local OS text to speech ( tts.exe / say etc ... )");
    meta.setAvailable(true);
    meta.addCategory("speech");
    meta.addDependency("com.microsoft", "tts", "1.1", "zip");
    return meta;
  }
  
}

