package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Amt203EncoderMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(Amt203EncoderMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Amt203Encoder");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("AMT203 Encoder - Absolute position encoder");
    meta.addCategory("encoder", "sensors");
    return meta;
  }
  
  
}

