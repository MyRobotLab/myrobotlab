package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GitMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GitMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public GitMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("used to manage source code");
    addCategory("programming");

    // EDL (new-style BSD) licensed
    addDependency("org.eclipse.jgit", "org.eclipse.jgit", "5.4.0.201906121030-r");
  }

}
