package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TestMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(TestMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Test");
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

