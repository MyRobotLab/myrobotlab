package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ImapEmailConnectorMeta {
  public final static Logger log = LoggerFactory.getLogger(ImapEmailConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.ImapEmailConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This connector will connect to an IMAP based email server and crawl the emails");
    meta.addCategory("monitor", "cloud", "network");
    meta.addDependency("javax.mail", "mail", "1.4.7");
    meta.setCloudService(true);

    return meta;
  }

  
  
}

