package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ImageDisplayMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(ImageDisplayMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.ImageDisplay");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("IBus serial protocol");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary

    // TEMPORARY CORE DEPENDENCIES !!! (for uber-jar)
    // meta.addDependency("orgId", "artifactId", "2.4.0");

    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }
  
}

