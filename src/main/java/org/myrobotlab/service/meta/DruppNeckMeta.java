package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DruppNeckMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DruppNeckMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public DruppNeckMeta() {

    addDescription("InMoov Drupp Neck Service");
    addCategory("robot");

    addPeer("up", "Servo", "Up servo");
    addPeer("middle", "Servo", "Middle servo");
    addPeer("down", "Servo", "Down servo");

    setAvailable(true);

  }

}
