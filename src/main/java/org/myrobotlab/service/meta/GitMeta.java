package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GitMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GitMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public GitMeta() {
    addDescription("used to manage source code");
    addCategory("programming");

    // EDL (new-style BSD) licensed
    addDependency("org.eclipse.jgit", "org.eclipse.jgit", "6.6.1.202309021850-r");
  }

}
