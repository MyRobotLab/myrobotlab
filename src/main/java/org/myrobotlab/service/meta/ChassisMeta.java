package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ChassisMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ChassisMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param name
   *          n
   * 
   */
  public ChassisMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("control platform");
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    addCategory("general");
    addPeer("left", "Motor", "left drive motor");
    addPeer("right", "Motor", "right drive motor");
    addPeer("joystick", "Joystick", "joystick control");
    addPeer("controller", "Sabertooth", "serial controller");

  }

}
