package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Mpu6050Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mpu6050Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public Mpu6050Meta() {
    addDescription("General MPU-6050 acclerometer and gyro");
    addCategory("microcontroller", "sensors");
    setSponsor("Mats");

  }

}
