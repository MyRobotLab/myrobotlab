package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoovTorsoMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(InMoovTorsoMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.InMoovTorso");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("InMoov Torso");
    meta.addCategory("robot");

    meta.addPeer("topStom", "Servo", "Top Stomach servo");
    meta.addPeer("midStom", "Servo", "Mid Stomach servo");
    meta.addPeer("lowStom", "Servo", "Low Stomach servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for torso");

    return meta;
  }
  
}

