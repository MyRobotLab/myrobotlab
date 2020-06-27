package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestCatcherMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(TestCatcherMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.TestCatcher");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("This service is used to test messaging");
    meta.setAvailable(false);
    meta.addCategory("testing", "framework");
    
    // meta.addPeer("subpeer", "TestCatcher", "comment"); Don't do recursive infinite loop
    meta.addPeer("subpeer", "TestThrower", "comment"); // Don't do recursive infinite loop
    meta.addPeer("globalPeer", "thrower01", "TestThrower", "comment"); // Don't do recursive infinite loop

    return meta;
  }
    
}

