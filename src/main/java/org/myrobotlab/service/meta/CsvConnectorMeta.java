package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class CsvConnectorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(CsvConnectorMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * 
   */
  public CsvConnectorMeta() {
    addDescription("This service crawls a csv file and publishes each row as a document");
    addCategory("ingest");
    addDependency("net.sf.opencsv", "opencsv", "2.3");
  }

}
