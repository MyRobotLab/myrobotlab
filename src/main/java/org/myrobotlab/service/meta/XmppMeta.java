package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class XmppMeta {
  public final static Logger log = LoggerFactory.getLogger(XmppMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Xmpp");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("xmpp service to access the jabber network");
    meta.addCategory("cloud","network");

    meta.addDependency("org.igniterealtime.smack", "smack-java7", "4.1.6");
    meta.addDependency("org.igniterealtime.smack", "smack-tcp", "4.1.6");
    meta.addDependency("org.igniterealtime.smack", "smack-im", "4.1.6");
    meta.addDependency("org.igniterealtime.smack", "smack-extensions", "4.1.6");

    return meta;
  }

  
}

