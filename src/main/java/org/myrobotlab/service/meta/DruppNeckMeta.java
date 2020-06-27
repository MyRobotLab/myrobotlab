package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DruppNeckMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(DruppNeckMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.DruppNeck");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("InMoov Drupp Neck Service");
    meta.addCategory("robot");

    meta.addPeer("up", "Servo", "Up servo");
    meta.addPeer("middle", "Servo", "Middle servo");
    meta.addPeer("down", "Servo", "Down servo");

    meta.setAvailable(true);

    return meta;
  }

  
}

