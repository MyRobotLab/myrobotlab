package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DiyServo2Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DiyServo2Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public DiyServo2Meta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("Controls a motor so that it can be used as a Servo with an encoder feedback");
    addCategory("control", "servo");
    addPeer("motor", "MotorDualPwm", "MotorControl service");
    addPeer("pid", "Pid", "PID service");

  }

}
