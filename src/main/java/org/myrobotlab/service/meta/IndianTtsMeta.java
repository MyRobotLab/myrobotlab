package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

public class IndianTtsMeta {
  public final static Logger log = LoggerFactory.getLogger(IndianTtsMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    
    ServiceType meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.IndianTts");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("Hindi text to speech support - requires keys");
    meta.setCloudService(true);
    meta.addCategory("speech","cloud");
    meta.setSponsor("moz4r");
    meta.addCategory("speech", "sound");
    meta.addPeer("httpClient", "HttpClient", "httpClient");
    
    meta.setRequiresKeys(true);

    return meta;
  }

  
}

