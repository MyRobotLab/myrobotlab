package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class XMLConnectorMeta {
  public final static Logger log = LoggerFactory.getLogger(XMLConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.XMLConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This is an XML Connector that will parse a large xml file into many small xml documents");
    meta.addCategory("filter");
    // FIXME - make a service page, and /python/service example
    meta.setAvailable(false);
    return meta;
  }
  
}

