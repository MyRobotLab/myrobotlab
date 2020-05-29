package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class RekognitionMeta {
  public final static Logger log = LoggerFactory.getLogger(RekognitionMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.Rekognition");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Amazon visual recognition cloud service");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    meta.addDependency("com.amazonaws", "aws-java-sdk-rekognition", "1.11.263");
    meta.setCloudService(true);
    meta.setRequiresKeys(true);
    meta.addCategory("vision","cloud");
    return meta;
  }
  
  
}

