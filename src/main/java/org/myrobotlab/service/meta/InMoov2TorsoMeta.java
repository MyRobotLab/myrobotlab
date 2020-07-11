package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2TorsoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2TorsoMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public InMoov2TorsoMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("InMoov Torso");
    addCategory("robot");

    addPeer("topStom", "Servo", "Top Stomach servo");
    addPeer("midStom", "Servo", "Mid Stomach servo");
    addPeer("lowStom", "Servo", "Low Stomach servo");
    addPeer("arduino", "Arduino", "Arduino controller for torso");

  }

}
