package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoovHeadMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoovHeadMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public InMoovHeadMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("InMoov Head Service");
    addCategory("robot");

    addPeer("jaw", "Servo", "Jaw servo");
    addPeer("eyeX", "Servo", "Eyes pan servo");
    addPeer("eyeY", "Servo", "Eyes tilt servo");
    addPeer("rothead", "Servo", "Head pan servo");
    addPeer("neck", "Servo", "Head tilt servo");
    addPeer("rollNeck", "Servo", "rollNeck Mod servo");
    addPeer("arduino", "Arduino", "Arduino controller for this arm");

  }

}
