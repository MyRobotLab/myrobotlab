package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class LeapMotionMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(LeapMotionMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public LeapMotionMeta() {

    addDescription("Leap Motion Service");
    addCategory("sensors", "telerobotics");
    addDependency("leapmotion", "leap", "2.1.3");

    // TODO: These will overwrite each other! we need to be selective for the
    // platform of what we deploy.
    // I believe the 32bit libraries would overwrite the 64bit libraries.
    // addDependency("leapmotion", "leap-linux32", "2.1.3", "zip");
    // addDependency("leapmotion", "leap-win32", "2.1.3", "zip");
    // 64 bit support only for now. until we can switch out dependencies based
    // on the current platform.
    addDependency("leapmotion", "leap-win64", "2.1.3", "zip");
    addDependency("leapmotion", "leap-mac64", "2.1.3", "zip");
    addDependency("leapmotion", "leap-linux64", "2.1.3", "zip");

  }

}
