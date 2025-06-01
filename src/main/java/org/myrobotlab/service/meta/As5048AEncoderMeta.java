package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class As5048AEncoderMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(As5048AEncoderMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public As5048AEncoderMeta() {
    addDescription("AS5048A Encoder - 14 bit - Absolute position encoder");
    addCategory("encoder", "sensors");
  }

}
