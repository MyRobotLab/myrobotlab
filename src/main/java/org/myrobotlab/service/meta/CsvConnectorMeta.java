package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CsvConnectorMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(CsvConnectorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.CsvConnector");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("This service crawls a csv file and publishes each row as a document");
    meta.addCategory("ingest");
    meta.addDependency("net.sf.opencsv", "opencsv", "2.3");
    return meta;
  }

  
}

