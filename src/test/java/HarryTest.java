import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.service.Solr;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.ProgramABTest;
import org.slf4j.Logger;
import org.myrobotlab.service.Runtime;

@Ignore
public class HarryTest {

  public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);
  
  @Test
  public void testHarry() throws InterruptedException, IOException, SolrServerException {

    LoggingFactory.init("WARN");
    // create memory
    Solr solr = (Solr)Runtime.start("solr", "Solr");
    solr.startEmbedded();
    createAIML();
    ProgramAB ab = (ProgramAB)Runtime.start("ab", "ProgramAB");
    ab.startSession("testbots",  "username", "test");

    // start the opencv service with the yolo filter.
    OpenCV cv = (OpenCV)Runtime.start("cv", "OpenCV");
    OpenCVFilterYolo yoloFilter = new OpenCVFilterYolo("yolo");
    cv.addFilter(yoloFilter);

    // attach the memory to the programab and the opencv service
    solr.attach(ab);
    solr.attach(cv);

    // Turn on the camera
    cv.capture();

    // watch for 5 seconds..
    Thread.sleep(5000);
    // ask program ab something

    
    SolrQuery mostRecentObjectQuery = new SolrQuery("*:*");
    mostRecentObjectQuery.setSort("index_date", ORDER.desc);
    mostRecentObjectQuery.addFacetField("label");

   QueryResponse res = solr.search(mostRecentObjectQuery);
   
   // most populate label
   for (Count v : res.getFacetField("label").getValues()) {
    System.out.println("Facet Label: " +  v.getName() + " count: " + v.getCount());
   }
   printResponse(res); 
   
   //Response qresp = ab.getResponse("What have you seen?");

   Runtime.start("gui", "SwingGui");
    
   // System.out.println("Any key");
   System.in.read();

  }

  private void printResponse(QueryResponse res) {
    // TODO Auto-generated method stub
    for (SolrDocument d : res.getResults()) {
      System.out.println("################################################");
      System.out.println(d);
    }
    
    for (org.apache.solr.client.solrj.response.FacetField f : res.getFacetFields()) {
      System.out.println("FACET: " + f.toString());
    }
    System.out.println("end");
  }

  
  
  private void createAIML() throws IOException {
    // TODO Auto-generated method stub

    // we should create a bot directory, and put sme aiml in it.
    // also maybe config..
    
    // TODO: pick a test subdirectory
    File f = new File ("testbots/bots/test/aiml");
    f.mkdirs();
    
    File aimlFile = new File("testbots/bots/test/aiml/test.aiml");
    FileWriter fw = new FileWriter(aimlFile);
    
    // create test aiml
    StringBuilder b = new StringBuilder();
    b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><aiml>");
    b.append("<category>");
    b.append("  <pattern>*</pattern>");
    b.append("  <template>Ok.</template>");
    b.append("</category>");
    b.append("</aiml>");
    
    fw.write(b.toString());

    fw.close();
    
    
    
  }
}
