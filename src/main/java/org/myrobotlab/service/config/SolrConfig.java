package org.myrobotlab.service.config;

public class SolrConfig extends ServiceConfig {

  // if you use embedded, the solrUrl is ignored
  public boolean embedded = true;
  // If embedded = false, then the following url will be used to search a solr cluster.
  // This will/should be replaced with the zkHost and a SolrCloud client.
  public String solrUrl = "http://localhost:8983/solr/collection1";

}
