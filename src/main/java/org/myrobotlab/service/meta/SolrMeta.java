package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SolrMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(SolrMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Solr");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Solr Service - Open source search engine");
    meta.addCategory("search");
    String solrVersion = "8.4.1";
    meta.addDependency("org.apache.solr", "solr-core", solrVersion);
    meta.exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    meta.addDependency("org.apache.solr", "solr-test-framework", solrVersion);
    meta.exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    meta.addDependency("org.apache.solr", "solr-solrj", solrVersion);
    meta.addDependency("commons-io", "commons-io", "2.5");
    // TODO: update this with the latest schema!
    // meta.addDependency("mrl-solr", "mrl-solr-data", "1.0", "zip");
    // log4j-slf4j conflicts with logback in solr 7.4.0+ (maybe earlier)
    meta.exclude("org.apache.logging.log4j", "log4j-slf4j-impl");
    // Dependencies issue
    meta.setAvailable(true);
    return meta;
  }
  
  
}

