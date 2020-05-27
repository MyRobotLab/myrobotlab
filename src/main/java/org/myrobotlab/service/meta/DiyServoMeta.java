package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DiyServoMeta {
  public final static Logger log = LoggerFactory.getLogger(DiyServoMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.DiyServo");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Controls a motor so that it can be used as a Servo");
    meta.addCategory("control", "servo");
    meta.addPeer("motor", "MotorDualPwm", "MotorControl service");
    meta.addPeer("pid", "Pid", "PID service");
    return meta;
  }

  
  
}

