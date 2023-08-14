package org.myrobotlab.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

@Ignore
public class HarryTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);

  private void createAIML() throws IOException {
    // TODO Auto-generated method stub
    // we should create a bot directory, and put sme aiml in it.
    // also maybe config..
    // TODO: pick a test subdirectory
    File f = new File("testbots/bots/test/aiml");
    f.mkdirs();
    File aimlFile = new File("testbots/bots/test/aiml/test.aiml");
    FileWriter fw = new FileWriter(aimlFile);
    // create test aiml
    String pattern = "*";
    String template = "ok";
    String category = createCategory(pattern, template);

    StringBuilder b = new StringBuilder();
    b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><aiml>");
    b.append(category);
    b.append("</aiml>");
    fw.write(b.toString());
    fw.close();
  }

  // TODO: make some generic aiml tools that can be reused
  private String createCategory(String pattern, String template) {
    StringBuilder b = new StringBuilder();
    b.append("<category>");
    b.append("  <pattern>");
    b.append(pattern);
    b.append("</pattern>");
    b.append("  <template>");
    b.append(template);
    b.append("</template>");
    b.append("</category>");
    return b.toString();
  }

  private void goLearnStuff(Solr solr, ProgramAB harry) throws InterruptedException {
    String rssUrl = "http://feeds.reuters.com/reuters/scienceNews";
    RSSConnector rss = (RSSConnector) Runtime.start("rss", "RSSConnector");
    rss.setRssUrl(rssUrl);
    rss.attachDocumentListener(solr.getName());

    Thread.sleep(1000);

    rss.startCrawling();

    // run a search and add a new category to programab.
    queryToCategory(solr, harry);
    // TODO: pass the category in directly.
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

  public void queryToCategory(Solr solr, ProgramAB ab) {

    SolrQuery qr = new SolrQuery("+title:* +description:*");
    QueryResponse resp = solr.search(qr);

    // we want to find the most recent docs from solr

    String pattern = "NEWS";

    StringBuilder templateBuilder = new StringBuilder();

    // now iterate over the docs and build a response
    // we'll set up a random response for AIML
    String preamble = "In the news ";
    templateBuilder.append(preamble);
    templateBuilder.append("<random>");
    for (SolrDocument d : resp.getResults()) {
      if (d.getFieldNames().contains("title")) {
        Object title = d.getFirstValue("title");
        templateBuilder.append("<li>");
        // get the title of the result.
        templateBuilder.append(title.toString().trim());
        templateBuilder.append("</li>");
        // if the last reponse was title.. the tell me more should return the
        // description.
        if (d.getFirstValue("description") != null) {
          // ab.addCategory("TELL ME MORE",
          // d.getFirstValue("description").toString().trim(), preamble +
          // title.toString());
        }
      }
    }
    templateBuilder.append("</random>");
    ab.addCategory(pattern, templateBuilder.toString());

    // now we want to build a category for just that

  }

  private void setupVirtualArduinos(String leftPort, String rightPort) throws IOException {
    // TODO Auto-generated method stub
    VirtualArduino leftVirtual = (VirtualArduino) Runtime.start("leftVirtual", "VirtualArduino");
    leftVirtual.getSerial().setTimeout(100);
    leftVirtual.connect(leftPort);
    VirtualArduino rightVirtual = (VirtualArduino) Runtime.start("rightVirtual", "VirtualArduino");
    rightVirtual.getSerial().setTimeout(100);
    rightVirtual.connect(rightPort);
  }

  // @Test
  public void testDynamic() throws SolrServerException, IOException, InterruptedException {

    // LoggingFactory.init("INFO");
    // create memory
    Solr solr = (Solr) Runtime.start("solr", "Solr");
    solr.startEmbedded();
    createAIML();
    ProgramAB harry = (ProgramAB) Runtime.start("harry", "ProgramAB");
    harry.startSession("testbots", "username", "test");

    goLearnStuff(solr, harry);

    Thread.sleep(1000);

    Runtime.start("gui", "SwingGui");

    System.out.println("Any key");
    System.in.read();

  }

  @Test
  public void testHarry() throws Exception {

    // LoggingFactory.init("WARN");
    // create memory
    Solr solr = (Solr) Runtime.start("solr", "Solr");
    solr.startEmbedded();
    createAIML();
    ProgramAB harry = (ProgramAB) Runtime.start("harry", "ProgramAB");
    harry.startSession("testbots", "username", "test");

    // start the opencv service with the yolo filter.
    OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
    OpenCVFilterYolo yoloFilter = new OpenCVFilterYolo("yolo");
    cv.addFilter(yoloFilter);

    // attach the memory to the programab and the opencv service
    solr.attach(harry);
    solr.attach(cv);

    // watch for 5 seconds..
    // Thread.sleep(5000);
    // ask program ab something

    SolrQuery mostRecentObjectQuery = new SolrQuery("*:*");
    mostRecentObjectQuery.setSort("index_date", ORDER.desc);
    mostRecentObjectQuery.addFacetField("label");

    QueryResponse res = solr.search(mostRecentObjectQuery);

    // most populate label
    for (Count v : res.getFacetField("label").getValues()) {
      System.out.println("Facet Label: " + v.getName() + " count: " + v.getCount());
    }
    printResponse(res);

    // Response qresp = ab.getResponse("What have you seen?");

    Runtime.start("gui", "SwingGui");
    // Runtime.start("webgui", "WebGui");

    // let's add some speech synthesis
    MarySpeech mouth = (MarySpeech) Runtime.createAndStart("mouth", "MarySpeech");
    mouth.setVoice("cmu-bdl-hsmm");

    // let's add some speech recognition

    WebkitSpeechRecognition ear = (WebkitSpeechRecognition) Runtime.createAndStart("i01.ear", "WebkitSpeechRecognition");
    // ear.addListener("publishText", python.name, "heard");
    ear.addMouth(mouth);

    HtmlFilter htmlfilter = (HtmlFilter) Runtime.createAndStart("htmlfilter", "HtmlFilter");
    // #####################################################################
    // # MRL Routing webkitspeechrecognition/ear -> program ab -> htmlfilter ->
    // mouth
    // ######################################################################
    ear.addTextListener(harry);
    harry.addTextListener(htmlfilter);
    htmlfilter.addTextListener(mouth);

    // TODO: start the virtual arudinos
    String leftPort = "COM99";
    String rightPort = "COM100";
    setupVirtualArduinos(leftPort, rightPort);

    InMoov2 i01 = (InMoov2) Runtime.createAndStart("i01", "InMoov2");
    i01.setMute(true);
    // i01.startAll();
    // if startInMoov:
    // i01.startAll(leftPort, rightPort)
    // else:
    i01.mouth = mouth;

    solr.attachAllInboxes();
    solr.attachAllOutboxes();

    ServoMixer servoMixer = (ServoMixer) Runtime.start("servoMixer", "ServoMixer");

    // lastly turn on the camera
    // Turn on the camera
    // cv.capture();

    goLearnStuff(solr, harry);

    System.out.println("Any key");
    System.in.read();

  }
}
