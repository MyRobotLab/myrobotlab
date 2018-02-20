package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.opencv.OpenCVFilterDL4J;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;

// @Ignore
public class VisionMemoryTest {

  @Test
  public void testVisionMemory() throws InterruptedException, SolrServerException, IOException {
    // basic log level stuff
    Runtime.setLogLevel("INFO");
    // for debugging
    Runtime.createAndStart("gui", "SwingGui");
    // start up the embedded solr server
    Solr solr = (Solr)Runtime.createAndStart("solr", "Solr");
    solr.startEmbedded();
    // now start up dl4j
    Deeplearning4j dl4j = (Deeplearning4j)Runtime.createAndStart("dl4j", "Deeplearning4j");
    dl4j.loadVGG16();
    // start up opencv
    OpenCV opencv = (OpenCV)Runtime.createAndStart("opencv", "OpenCV");
    // add the dl4j filter to opencv
    // TODO: add an attach pattern for the opencv filters
    OpenCVFilterDL4J dl4jfilter = new OpenCVFilterDL4J("dl4jfilter");
    // TODO: the dl4j filter should be able to attach dl4j.. right now it spawns dl4j as a service.
    opencv.addFilter(dl4jfilter);
    // attach dl4j to solr, we expect publish classifications to come out of dl4j

    solr.attach(dl4j);
    // Start capturing frames, this should go though the dl4j filter, and publish the classifications to solr
    opencv.capture();
    int i = 0;
    while (true) {
      i++;
      Thread.sleep(1000);
      // let's report on metadata about what we've seen
      SolrQuery query = new SolrQuery();
      query.setQuery("*:*");
      query.setFacet(true);
      query.addFacetField("object");
      QueryResponse qr = solr.search(query);		

      long numRows=  qr.getResults().getNumFound();
      System.out.println("Rows : " + numRows);
      if (numRows > 0) {
        for (SolrDocument doc : qr.getResults()) {
          System.out.println(doc);
        }

        for (FacetField ff : qr.getFacetFields()) {
          for (Count c : ff.getValues()) {
            System.out.println(c.getName() + " " + c.getCount());
          }
        }
      }

      if(i > 100) {
        System.out.println("Exiting");
        System.exit(0);
      }
    }




  }
}
