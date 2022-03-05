package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VirtualArduinoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VirtualArduinoMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public VirtualArduinoMeta() {

    addDescription("virtual hardware of for the Arduino!");
    setAvailable(true);
    addPeer("uart", "Serial", "serial device for this Arduino");
    addCategory("simulator");

  }

}
