package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class JavaScriptMeta {
  public final static Logger log = LoggerFactory.getLogger(JavaScriptMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.JavaScript");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("native jvm javascript engine, which allows execution of javascript through exec method");
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("programming");
    return meta;
  }
  
}

