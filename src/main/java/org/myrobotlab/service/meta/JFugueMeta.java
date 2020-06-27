package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JFugueMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(JFugueMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.JFugue");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("service wrapping Jfugue, used for music and sound generation");
    meta.addCategory("sound","music");
    // org="jfugue" name="jfugue" rev="5.0.7
    meta.addDependency("jfugue", "jfugue", "5.0.7");
    return meta;
  }

  
}

