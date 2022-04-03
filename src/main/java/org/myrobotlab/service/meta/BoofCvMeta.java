package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class BoofCvMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(BoofCvMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public BoofCvMeta() {

    addDescription("a very portable vision library using pure Java");
    setAvailable(true);
    // add dependency if necessary
    addDependency("org.boofcv", "boofcv-core", "0.31");
    addDependency("org.boofcv", "boofcv-swing", "0.31");
    addDependency("org.boofcv", "boofcv-openkinect", "0.31");
    addCategory("vision", "video");
  }

}
