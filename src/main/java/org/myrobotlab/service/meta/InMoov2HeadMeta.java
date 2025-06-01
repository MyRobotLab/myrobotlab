package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2HeadConfig;
import org.myrobotlab.service.config.MouthControlConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2HeadMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2HeadMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public InMoov2HeadMeta() {
    addDescription("The inmoov2 head");
  }



}
