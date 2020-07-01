package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class PidMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(PidMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Pid");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("A proportional integral derivative controller (Pid controller) commonly used in industrial control systems");
    meta.addCategory("control", "industrial");
    return meta;
  }

  
}

