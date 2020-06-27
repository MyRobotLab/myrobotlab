package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoProMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(GoProMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.GoPro");
    Platform platform = Platform.getLocalInstance();
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("video");
    meta.addPeer("http", "HttpClient", "Http for GoPro control");
    meta.addDescription("Go pro camera support");
    return meta;
  }

  
  
}

