package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class EddieControlBoardMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(EddieControlBoardMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public EddieControlBoardMeta() {
    addDescription("microcontroller designed for robotics");
    addCategory("microcontroller");
    // John Harland no longer uses this hardware
    setAvailable(false);

    // put peer definitions in
    addPeer("serial", "Serial", "serial");
    addPeer("keyboard", "Keyboard", "serial");
    addPeer("webgui", "WebGui", "webgui");
    // meta.addPeer("remote", "RemoteAdapter", "remote interface");
    addPeer("joystick", "Joystick", "joystick");

  }

}
