package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SerialMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(SerialMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Serial");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("reads and writes data to a serial port");
    meta.addCategory("sensors", "control");
    meta.addDependency("org.scream3r", "jssc", "2.8.0-1");
    meta.setLicenseGplV3(); // via jssc
    return meta;
  }

  
}

