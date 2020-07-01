package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class IpCameraMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(IpCameraMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.IpCamera");
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

