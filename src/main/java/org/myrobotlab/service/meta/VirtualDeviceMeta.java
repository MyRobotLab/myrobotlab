package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VirtualDeviceMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VirtualDeviceMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public VirtualDeviceMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("a service which can create virtual serial ports and behaviors implemented in python for them");
    addCategory("testing");
    // put peer definitions in
    addPeer("uart", "Serial", "uart");
    addPeer("logic", "Python", "logic to implement");

    // this is used for testing, and does not need to be tested
    setAvailable(false);

  }

}
