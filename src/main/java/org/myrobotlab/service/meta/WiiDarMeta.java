package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WiiDarMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(WiiDarMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.WiiDar");
    Platform platform = Platform.getLocalInstance();
    // meta.addDependency("wiiuse.wiimote", "0.12b");

    meta.addDescription("WiiDar.... who dar?  WiiDar!");
    meta.addDependency("wiiusej", "wiiusej", "wiiusej");
    meta.addCategory("sensors");
    // no longer have hardware for this ...
    meta.setAvailable(false);
    return meta;
  }
  
}

