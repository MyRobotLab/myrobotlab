package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class LeapMotionMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(LeapMotionMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.LeapMotion");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Leap Motion Service");
    meta.addCategory("sensors", "telerobotics");
    meta.addDependency("leapmotion", "leap", "2.1.3");

    // TODO: These will overwrite each other!  we need to be selective for the platform of what we deploy.
    // I believe the 32bit libraries would overwrite the 64bit libraries. 
    // meta.addDependency("leapmotion", "leap-linux32", "2.1.3", "zip");
    // meta.addDependency("leapmotion", "leap-win32", "2.1.3", "zip");
    // 64 bit support only for now.  until we can switch out dependencies based on the current platform.
    meta.addDependency("leapmotion", "leap-win64", "2.1.3", "zip");
    meta.addDependency("leapmotion", "leap-mac64", "2.1.3", "zip");
    meta.addDependency("leapmotion", "leap-linux64", "2.1.3", "zip");
    
    return meta;
  }
  
  
}

