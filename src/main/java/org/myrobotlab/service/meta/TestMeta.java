package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class TestMeta {
  public final static Logger log = LoggerFactory.getLogger(TestMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Test");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Testing service");
    meta.addCategory("testing");
    meta.addPeer("http", "HttpClient", "to interface with Service pages");
    meta.setAvailable(false);

    meta.addDependency("junit", "junit", "4.12");
    // meta.addPeer("python", "Python", "python to excercise python scripts");
    return meta;
  }

  
}

