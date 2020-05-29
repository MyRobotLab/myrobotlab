package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DatabaseConnectorMeta {
  public final static Logger log = LoggerFactory.getLogger(DatabaseConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.DatabaseConnector");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This service will run a select statement against a database and return the rows as documents to be published");
    meta.addCategory("ingest");
    return meta;
  }

  
}

