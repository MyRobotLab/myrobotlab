package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebcamMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(WebcamMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Webcam");
    Platform platform = Platform.getLocalInstance();
    // setAutoOpenMode(true);
    
    meta.addDescription("used as a general webcam");
    meta.addCategory("video");
    return meta;
  }
  
  
}

