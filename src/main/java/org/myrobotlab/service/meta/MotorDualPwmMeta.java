package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class MotorDualPwmMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MotorDualPwmMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          mn
   * 
   */
  public MotorDualPwmMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("Motor service which support 2 pwr pwm pins clockwise and counterclockwise");
    addCategory("motor");

  }

}
