package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WolframAlphaMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WolframAlphaMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WolframAlphaMeta() {

    addCategory("ai", "cloud");
    // TODO http should be removed as a dependency and added as a Peer

    addDescription("Run queries against wolfram alpha!");
    addDependency("WolframAlpha", "WolframAlpha", "1.1");
    setCloudService(true);

  }

}
