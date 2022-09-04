package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OscMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OscMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies , and all other meta data related to the service.
   */
  public OscMeta() {

    addDescription("Service for the Open Sound Control using the JavaOsc library");
    setAvailable(true); // false if you do not want it viewable in a gui
    setLink("http://www.illposed.com/software/javaosc.html");
    // add dependency if necessary
    addDependency("com.illposed.osc", "javaosc-core", "0.4");
    addCategory("network", "music");

  }

}
