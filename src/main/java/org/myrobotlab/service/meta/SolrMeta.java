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
    String solrVersion = "8.11.2";
    String luceneVersion = solrVersion;
    addDependency("org.apache.lucene", "lucene-core", luceneVersion);
    addDependency("org.apache.solr", "solr-core", solrVersion);
    exclude("log4j", "log4j");
    exclude("org.apache.logging.log4j", "log4j-core");
    exclude("org.apache.logging.log4j", "log4j-web");
    exclude("org.apache.logging.log4j", "log4j-1.2-api");
    exclude("org.apache.logging.log4j", "log4j-api");
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    exclude("com.fasterxml.jackson.core", "jackson-core");
    exclude("com.fasterxml.jackson.core", "jackson-databind");
    exclude("com.fasterxml.jackson.core", "jackson-annotations");

    addDependency("org.apache.solr", "solr-test-framework", solrVersion);
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    exclude("log4j", "log4j");
    exclude("org.apache.logging.log4j", "log4j-core");
    exclude("org.apache.logging.log4j", "log4j-web");
    exclude("org.apache.logging.log4j", "log4j-1.2-api");
    exclude("org.apache.logging.log4j", "log4j-api");
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");

    exclude("com.fasterxml.jackson.core", "jackson-core");
    exclude("com.fasterxml.jackson.core", "jackson-databind");
    exclude("com.fasterxml.jackson.core", "jackson-annotations");

    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.13.3");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.13.3");

    addDependency("org.apache.solr", "solr-solrj", solrVersion);

    exclude("com.fasterxml.jackson.core", "jackson-core");
    exclude("com.fasterxml.jackson.core", "jackson-databind");
    exclude("com.fasterxml.jackson.core", "jackson-annotations");

    addDependency("commons-io", "commons-io", "2.7");

    // TODO: update this with the latest schema!
    // addDependency("mrl-solr", "mrl-solr-data", "1.0", "zip");
    // log4j-slf4j conflicts with logback in solr 7.4.0+ (maybe earlier)
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    // Dependencies issue
    setAvailable(true);

  }

}
