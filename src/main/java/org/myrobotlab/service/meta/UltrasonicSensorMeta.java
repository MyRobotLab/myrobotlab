package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class UltrasonicSensorMeta  extends MetaData {
  private static final long serialVersionUID = 1L;
public final static Logger log = LoggerFactory.getLogger(UltrasonicSensorMeta.class);
  
  /**
   * This class is contains all the meta data details of a service.
   * It's peers, dependencies, and all other meta data related to the service.
   * 
   */
  public UltrasonicSensorMeta() {

    
    Platform platform = Platform.getLocalInstance();
    
   addDescription("ranging sensor");
   addCategory("sensors");
   addPeer("controller", "Arduino", "default sensor controller will be an Arduino");
    
  }
  
  
}

