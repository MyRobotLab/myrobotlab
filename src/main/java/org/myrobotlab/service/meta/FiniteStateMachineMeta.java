package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class FiniteStateMachineMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachineMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public FiniteStateMachineMeta() {
    addDescription("general service which can create and maintaine multiple finite state machines");
    // addDependency("orgId", "artifactId", "2.4.0");
    addCategory("general", "ai");

    addDependency("com.github.pnavais", "state-machine", "1.2.0");
  }

}
