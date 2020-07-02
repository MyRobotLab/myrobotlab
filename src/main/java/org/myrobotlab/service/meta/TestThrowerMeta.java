package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestThrowerMeta  extends MetaData {
  private static final long serialVersionUID = 1L;
public final static Logger log = LoggerFactory.getLogger(TestThrowerMeta.class);
  
  /**
   * This class is contains all the meta data details of a service.
   * It's peers, dependencies, and all other meta data related to the service.
   * 
   */
  public TestThrowerMeta() {

    
    Platform platform = Platform.getLocalInstance();
    
   addDescription("TestThrower is used with TestCatcher to test messaging");
   setAvailable(false);
   addCategory("testing", "framework");

    
  }
  
}

