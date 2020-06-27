package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SensorMonitorMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(SensorMonitorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.SensorMonitor");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("sensor monitor - capable of displaying sensor information in a crude oscilliscope fasion");
    meta.addCategory("sensors", "display");

    return meta;
  }

  
}

