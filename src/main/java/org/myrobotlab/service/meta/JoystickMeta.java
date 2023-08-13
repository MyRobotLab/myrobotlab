package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class JoystickMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(JoystickMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public JoystickMeta() {

    Platform platform = Platform.getLocalInstance();
    addDescription("service allows interfacing with a keyboard, joystick or gamepad");
    addCategory("control", "telerobotics");
    addDependency("net.java.jinput", "jinput", "2.0.9");

    log.info("Joystick.getMetaData {}  isArm() {}", platform, platform.isArm());
    if (platform.isArm()) {
      log.info("adding armv7 native dependencies");
      addDependency("jinput-natives", "jinput-natives-armv7.hfp", "2.0.7", "zip");
    } else {
      log.info("adding jinput native dependencies");
      addDependency("jinput-natives", "jinput-natives", "2.0.7", "zip");
    }
    // addDependency("net.java.jinput", "jinput-platform", "2.0.7");
    // addArtifact("net.java.jinput", "natives-windows");
    // addArtifact("net.java.jinput", "natives-linux");
    // addArtifact("")

  }

}
