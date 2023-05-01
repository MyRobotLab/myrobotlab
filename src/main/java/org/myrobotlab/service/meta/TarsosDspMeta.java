package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TarsosDspMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TarsosDspMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TarsosDspMeta() {

    addDescription("digital signal processing - used for audio effects, although it could have many other uses");
    setAvailable(true); // false if you do not want it viewable in a gui
    // addDependency("com.github.axet", "TarsosDSP", "2.4-1");
    addCategory("general");

  }

}
