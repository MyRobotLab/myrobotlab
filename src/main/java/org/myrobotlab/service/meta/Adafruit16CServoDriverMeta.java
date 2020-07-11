package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Adafruit16CServoDriverMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriverMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * 
   * 
   */
  public Adafruit16CServoDriverMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("controls 16 pwm pins for 16 servos/LED or 8 motors");
    addCategory("shield", "servo", "pwm");
    setSponsor("Mats");
    // addDependency("com.pi4j.pi4j", "1.1-SNAPSHOT");
    /*
     * addPeer("arduino", "Arduino", "our Arduino");addPeer("raspi", "RasPi",
     * "our RasPi");
     */

  }

}
