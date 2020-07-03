package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoovEyelidsMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoovEyelidsMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public InMoovEyelidsMeta() {

    Platform platform = Platform.getLocalInstance();
    addDescription("InMoov Eyelids");
    addCategory("robot");

    addPeer("eyelidleft", "Servo", "eyelidleft or both servo");
    addPeer("eyelidright", "Servo", "Eyelid right servo");

  }

}
