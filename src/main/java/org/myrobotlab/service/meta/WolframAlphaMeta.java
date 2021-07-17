package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WolframAlphaMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WolframAlphaMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public WolframAlphaMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addCategory("ai", "cloud");
    // TODO http should be removed as a dependency and added as a Peer

    addDescription("Run queries against wolfram alpha!");
    addDependency("WolframAlpha", "WolframAlpha", "1.1");

    // FIXME - add Mrl Service HttpClient Peer - don't include dependency
    // directly
    /*
     * - currently Runtime provides these dependencies
     * addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
     * addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
     */

    setCloudService(true);

  }

}
