package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestCatcherMeta  extends MetaData {
  private static final long serialVersionUID = 1L;
public final static Logger log = LoggerFactory.getLogger(TestCatcherMeta.class);
  
  /**
   * This class is contains all the meta data details of a service.
   * It's peers, dependencies, and all other meta data related to the service.
   * 
   */
  public TestCatcherMeta() {

    
    Platform platform = Platform.getLocalInstance();
    
   addDescription("This service is used to test messaging");
   setAvailable(false);
   addCategory("testing", "framework");
    
    //addPeer("subpeer", "TestCatcher", "comment"); Don't do recursive infinite loop
   addPeer("subpeer", "TestThrower", "comment"); // Don't do recursive infinite loop
   addPeer("globalPeer", "thrower01", "TestThrower", "comment"); // Don't do recursive infinite loop

    
  }
    
}

