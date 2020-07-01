package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class XmppMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(XmppMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Xmpp");
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

