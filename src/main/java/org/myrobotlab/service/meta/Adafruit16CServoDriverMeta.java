package org.myrobotlab.service.meta;

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
   * @param name
   *          n
   * 
   */
  public Adafruit16CServoDriverMeta() {

    addDescription("controls 16 pwm pins for 16 servos/LED or 8 motors");
    addCategory("shield", "servo", "pwm");
    setSponsor("Mats");

  }

}
