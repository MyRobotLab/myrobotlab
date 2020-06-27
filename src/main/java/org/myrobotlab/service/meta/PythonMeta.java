package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class PythonMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(PythonMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Python");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("the Jython script engine compatible with pure Python 2.7 scripts");
    meta.addCategory("programming", "control");

    meta.includeServiceInOneJar(true);
    meta.addDependency("org.python", "jython-standalone", "2.7.1");
    return meta;
  }

  
  
}

