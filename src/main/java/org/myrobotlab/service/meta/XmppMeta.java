package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class XmppMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(XmppMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public XmppMeta() {

    addDescription("xmpp service to access the jabber network");
    addCategory("cloud", "network");

    addDependency("org.igniterealtime.smack", "smack-java7", "4.1.6");
    addDependency("org.igniterealtime.smack", "smack-tcp", "4.1.6");
    addDependency("org.igniterealtime.smack", "smack-im", "4.1.6");
    addDependency("org.igniterealtime.smack", "smack-extensions", "4.1.6");

  }

}
