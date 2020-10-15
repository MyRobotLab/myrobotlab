package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SolrMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SolrMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public SolrMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("Solr Service - Open source search engine");
    addCategory("search");
    String solrVersion = "8.6.3";
    addDependency("org.apache.solr", "solr-core", solrVersion);
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    addDependency("org.apache.solr", "solr-test-framework", solrVersion);
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    addDependency("org.apache.solr", "solr-solrj", solrVersion);
    addDependency("commons-io", "commons-io", "2.5");
    // TODO: update this with the latest schema!
    // addDependency("mrl-solr", "mrl-solr-data", "1.0", "zip");
    // log4j-slf4j conflicts with logback in solr 7.4.0+ (maybe earlier)
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    // Dependencies issue
    setAvailable(true);

  }

}
