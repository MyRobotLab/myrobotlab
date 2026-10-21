package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WikiDataFetcherMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WikiDataFetcherMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WikiDataFetcherMeta() {

    addDescription("This service grab data from wikidata website");
    addCategory("ai");
    setSponsor("beetlejuice");
    addDependency("org.wikidata.wdtk", "wdtk-client", "0.8.0");
    exclude("org.slf4j", "*");
    // force using httpClient service httpcomponents version
    exclude("org.apache.httpcomponents", "*");

    addDependency("org.apache.commons", "commons-compress", "1.21");
    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.17.1");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.1");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.17.1");
    setCloudService(true);

  }

}
