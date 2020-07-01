package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class KeyboardMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(KeyboardMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Keyboard");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("keyboard interface");
    meta.addCategory("control");

    meta.addDependency("com.1stleg", "jnativehook", "2.0.3");

    return meta;
  }

  
}

