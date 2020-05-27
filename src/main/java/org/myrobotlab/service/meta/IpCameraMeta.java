package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class IpCameraMeta {
  public final static Logger log = LoggerFactory.getLogger(IpCameraMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.IpCamera");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("control and video stream capture for generic ip camera");
    meta.addCategory("video");
    // FIXME - should be webcam dependency not opencv !
    // meta.addDependency("org.bytedeco.javacpp","1.1");

    // FIXME - should just add IpFrameGrabber and drop the dependency !!!
    // meta.addDependency("org.bytedeco", "javacv-platform", "1.3.3");
    return meta;
  }
}

