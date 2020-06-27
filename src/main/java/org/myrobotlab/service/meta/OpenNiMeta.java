package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OpenNiMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(OpenNiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.OpenNi");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("OpenNI Service - 3D sensor");
    meta.addCategory("video", "vision", "sensors", "telerobotics");
    meta.addPeer("streamer", "streamer", "VideoStreamer", "video streaming service for webgui.");
    // meta.addDependency("com.googlecode.simpleopenni", "1.96");

    meta.addDependency("simpleopenni", "openni", "1.96");
    meta.addDependency("org.myrobotlab.openni", "openni-deps", "0.1", "zip");
    
    meta.addDependency("javax.vecmath", "vecmath", "1.5.2");
    meta.addDependency("java3d", "j3d-core", "1.3.1");
    meta.addDependency("java3d", "j3d-core-utils", "1.3.1");

    return meta;
  }
  
  
}

