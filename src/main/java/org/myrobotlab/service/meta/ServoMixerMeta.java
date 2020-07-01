package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ServoMixerMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(ServoMixerMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.ServoMixer");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("ServoMixer - most just a swing gui that allows for simple movements of all servos in one gui panel.");
    return meta;
  }
}

