package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SolrMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SolrMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public SolrMeta() {

    addDescription("Solr Service - Open source search engine");
    addCategory("search");
    String solrVersion = "9.2.0";
    String luceneVersion = "9.4.2";
    addDependency("org.apache.lucene", "lucene-core", luceneVersion);
    addDependency("org.apache.solr", "solr-core", solrVersion);
    exclude("log4j", "*");
    exclude("org.apache.logging.log4j", "*");
    exclude("com.fasterxml.jackson.core", "*");
    exclude("io.netty", "*"); // prevent it from bringing in an old version of netty

    addDependency("org.apache.solr", "solr-test-framework", solrVersion);
    exclude("log4j", "*");
    exclude("org.apache.logging.log4j", "*");
    exclude("com.fasterxml.jackson.core", "*");
    exclude("io.netty", "*");

    addDependency("org.apache.solr", "solr-solrj", solrVersion);
    exclude("com.fasterxml.jackson.core", "*");
    exclude("io.netty", "*");

    addDependency("commons-io", "commons-io", "2.7");

    // TODO: update this with the latest schema!
    // addDependency("mrl-solr", "mrl-solr-data", "1.0", "zip");
    // log4j-slf4j conflicts with logback in solr 7.4.0+ (maybe earlier)
    exclude("org.apache.logging.log4j", "*");
    
    // force correct version of netty
    addDependency("io.netty", "netty-all", "4.1.82.Final");

    // Dependencies issue
    setAvailable(true);

  }

}
