package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class EddieControlBoardMeta {
  public final static Logger log = LoggerFactory.getLogger(EddieControlBoardMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.EddieControlBoard");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("microcontroller designed for robotics");
    meta.addCategory("microcontroller");
    // John Harland no longer uses this hardware
    meta.setAvailable(false);

    // put peer definitions in
    meta.addPeer("serial", "Serial", "serial");
    meta.addPeer("keyboard", "Keyboard", "serial");
    meta.addPeer("webgui", "WebGui", "webgui");
    //meta.addPeer("remote", "RemoteAdapter", "remote interface");
    meta.addPeer("joystick", "Joystick", "joystick");

    return meta;
  }
  
}

