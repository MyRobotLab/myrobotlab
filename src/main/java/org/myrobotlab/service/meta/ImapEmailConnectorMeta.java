package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ImapEmailConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ImapEmailConnectorMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public ImapEmailConnectorMeta() {
    addDescription("This connector will connect to an IMAP based email server and crawl the emails");
    addCategory("monitor", "cloud", "network");
    addDependency("javax.mail", "mail", "1.4.7");
    setCloudService(true);

  }

}
