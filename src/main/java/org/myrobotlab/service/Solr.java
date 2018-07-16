package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.service.ProgramAB.Response;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.TextListener;
import org.slf4j.Logger;

/**
 * SolrService - MyRobotLab This is an integration of Solr into MyRobotLab. Solr
 * is the popular, blazing-fast, open source enterprise search platform built on
 * Apache Lucene.
 * 
 * This service exposes a the solrj client to be able to add documents and query
 * a solr server that is running.
 * 
 * For More info about Solr see http://lucene.apache.org/solr/
 * 
 * @author kwatters
 *
 */
public class Solr extends Service implements DocumentListener, TextListener, MessageListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Solr.class);

  public String solrUrl = "http://localhost:8983/solr";

  transient private SolrClient solrServer;

  public boolean commitOnFlush = true;

  EmbeddedSolrServer embeddedSolrServer = null;
  
  
  public void startEmbedded() throws SolrServerException, IOException {
    File resDir = new File("src/main/resources/resource/Solr");
    if (!resDir.exists()) {
      // we're using the "Solr" directory
      startEmbedded("Solr");
    } else {
      startEmbedded("src/main/resources/resource/Solr");
    }
    
  }
  
  public void startEmbedded(String path) throws SolrServerException, IOException {
    // create and load the cores
    Path solrHome = Paths.get(path);
    System.out.println(solrHome.toFile().getAbsolutePath());
    Path solrXml = solrHome.resolve("solr.xml");
    CoreContainer cores = CoreContainer.createAndLoad(solrHome, solrXml);
    for (String coreName : cores.getAllCoreNames()) {
      System.out.println("Core " + coreName);
    }
    embeddedSolrServer = new EmbeddedSolrServer(cores, "core1");
//    SolrInputDocument doc = new SolrInputDocument();
//    doc.setField("id", "doc_1");
//    embeddedSolrServer.add(doc);
//    embeddedSolrServer.commit();
//    SolrQuery q = new SolrQuery();
//    QueryRequest r = new QueryRequest(q);
//    q.setQuery("id:doc_2");
//    QueryResponse qr = embeddedSolrServer.query(q);
//    System.out.println(qr.getResults().getNumFound());
//    // shutdown & cleanup
//    embeddedSolrServer.getCoreContainer().shutdown();
//    embeddedSolrServer.close();
//    System.out.println("Done.");
    
  }
  
  /*
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   */

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Solr solr = (Solr) Runtime.start("solr", "Solr");
      solr.startEmbedded();
      SwingGui gui = (SwingGui)Runtime.start("gui", "SwingGui");
      
      // Create a test document
      SolrInputDocument doc = new SolrInputDocument();
      doc.setField("id", "Doc1");
      doc.setField("title", "My title");
      doc.setField("content", "This is the text field, for a sample document in myrobotlab.  ");
      // add the document to the index
      solr.addDocument(doc);
      // commit the index
      solr.commit();
      // search for the word myrobotlab
      String queryString = "myrobotlab";
      QueryResponse resp = solr.search(queryString);
      for (int i = 0; i < resp.getResults().size(); i++) {
        System.out.println("---------------------------------");
        System.out.println("-- Printing Result number :" + i);
        // grab a document out of the result set.
        SolrDocument d = resp.getResults().get(i);
        // iterate over the fields on the returned document
        for (String fieldName : d.getFieldNames()) {

          System.out.print(fieldName + "\t");
          // fields can be multi-valued
          for (Object value : d.getFieldValues(fieldName)) {
            System.out.print(value);
            System.out.print("\t");
          }
          System.out.println("");
        }
      }
      System.out.println("---------------------------------");
      System.out.println("Done.");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Solr(String n) {
    super(n);
  }

  public void addDocument(SolrInputDocument doc) {
    try {
      if (embeddedSolrServer != null) {
        embeddedSolrServer.add(doc);
      } else {
        solrServer.add(doc);
      }
    } catch (SolrServerException e) {
      // TODO : retry?
      log.warn("An exception occurred when trying to add document to the index.", e);
    } catch (IOException e) {
      // TODO : maybe retry?
      log.warn("A network exception occurred when trying to add document to the index.", e);
    }
  }

  /**
   * Add a solr document to the index
   * @param docs a collection of solr input docs to add to solr.
   */
  public void addDocuments(Collection<SolrInputDocument> docs) {
    try {
      if (embeddedSolrServer != null) {
        embeddedSolrServer.add(docs);
      } else {
        solrServer.add(docs);
      }
    } catch (SolrServerException e) {
      log.warn("An exception occurred when trying to add documents to the index.", e);
    } catch (IOException e) {
      log.warn("A network exception occurred when trying to add documents to the index.", e);
    }
  }

  /**
   * Commit the solr index and make documents that have been submitted become
   * searchable.
   */
  public void commit() {
    try {
      if (embeddedSolrServer != null) {
        embeddedSolrServer.commit();
      } else {
        solrServer.commit();
      }
    } catch (SolrServerException e) {
      log.warn("An exception occurred when trying to commit the index.", e);
    } catch (IOException e) {
      log.warn("A network exception occurred when trying to commit the index.", e);
    }
  }

  public void deleteDocument(String docId) {
    try {
      if (embeddedSolrServer!= null) {
        embeddedSolrServer.deleteById(docId);
      } else {
        solrServer.deleteById(docId);
      }
    } catch (Exception e) {
      // TODO better error handling/reporting?
      log.warn("An exception occurred when deleting doc", e);
    }
  }

  /**
   * @return The url for the solr instance you wish to query. Defaults to
   * http://localhost:8983/solr
   */

  public String getSolrUrl() {
    return solrUrl;
  }

  /**
   * Optimize the index, if the index gets very fragmented, this helps optimize
   * performance and helps reclaim some disk space.
   */
  public void optimize() {
    try {
      // TODO: expose the num segements and stuff?
      solrServer.optimize();
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      log.warn("An error occurred when optimizing the index.", e);
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.warn("A network error occurred when optimizing the index, solr down?", e);
      e.printStackTrace();
    }
  }

  /**
   * Pass in custom solr query parameters and execute that query.
   * 
   * @param query the query to execute
   * @return a query response from solr
   */
  public QueryResponse search(SolrQuery query) {
    QueryResponse resp = null;
    try {
      if (embeddedSolrServer != null) {
        resp = embeddedSolrServer.query(query);
      } else {
        resp = solrServer.query(query);
      }
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return resp;
  }

  /*
   * Default query to fetch the top 10 documents that match the query request.
   * 
   */
  public QueryResponse search(String queryString) {
    // default to 10 hits returned.
    return search(queryString, 10, 0);
  }

  /*
   * Default query to fetch the top 10 documents that match the query request.
   * 
   */
  public QueryResponse search(String queryString, int rows, int start) {
    SolrQuery query = new SolrQuery();
    query.set("q", queryString);
    query.setRows(rows);
    query.setStart(start);
    QueryResponse resp = null;
    try {
      if (embeddedSolrServer != null) {
        resp = embeddedSolrServer.query(query);
      } else {
        resp = solrServer.query(query);
      }
    } catch (SolrServerException | IOException e) {
      log.warn("Search failed with exception", e);
    }
    invoke("publishResults", resp);
    // invoke("publishResults");
    return resp;
  }

  // public String publishResults() {
  // return "this is a foo.";
  // };
  public QueryResponse publishResults(QueryResponse resp) {
    return resp;
  };

  /*
   * Set the url for the solr instance to communicate with.
   * 
   */
  public void setSolrUrl(String solrUrl) {
    this.solrUrl = solrUrl;
    // TODO: this isn't good to include behavior here but
    // if someone switches the url, we want to re-create the solr server.
    // this breaks the bean pattern a bit..
    if (solrServer != null) {
      solrServer = new HttpSolrClient.Builder().withBaseSolrUrl(solrUrl).build();
    }
  }

  @Override
  public void startService() {
    super.startService();
    solrServer = new HttpSolrClient.Builder().withBaseSolrUrl(solrUrl).build();
  }

  @Override
  public ProcessingStatus onDocuments(List<Document> docs) {
    // Convert the input document to a solr input docs and send it!
    ArrayList<SolrInputDocument> docsToSend = new ArrayList<SolrInputDocument>();
    for (Document d : docs) {
      docsToSend.add(convertDocument(d));
    }
    try {
      if (embeddedSolrServer != null) {
        embeddedSolrServer.add(docsToSend);
      } else {
        solrServer.add(docsToSend);
      }
      return ProcessingStatus.OK;
    } catch (SolrServerException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return ProcessingStatus.DROP;
    }
  }

  private SolrInputDocument convertDocument(Document doc) {
    SolrInputDocument solrDoc = new SolrInputDocument();
    solrDoc.setField("id", doc.getId());
    for (String fieldName : doc.getFields()) {
      for (Object o : doc.getField(fieldName)) {
        if (o != null) {
          solrDoc.addField(fieldName, o);
          // let's implicitly add a text_en version of the field.
          // TODO: understand language detection on the field and dynamically specficy which field type to use.
          solrDoc.addField(fieldName + "_txt_en", o);
        }
      }
    }
    return solrDoc;
  }

  public ProcessingStatus onDocument(Document doc) {
    // always be batching when sending docs.
    ArrayList<Document> docs = new ArrayList<Document>();
    docs.add(doc);
    return onDocuments(docs);
  }

  @Override
  public boolean onFlush() {
    // NoOp currently, but at some point if we change how this service batches
    // it's
    // add messages to solr, we could revisit this.
    // or maybe issue a commit here? I hate committing the index so frequently,
    // but maybe it's ok.
    if (commitOnFlush) {
      commit();
    }
    return false;
  }

  public boolean isCommitOnFlush() {
    return commitOnFlush;
  }

  public void setCommitOnFlush(boolean commitOnFlush) {
    this.commitOnFlush = commitOnFlush;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Solr.class.getCanonicalName());
    meta.addDescription("Solr Service - Open source search engine");
    meta.addCategory("data", "search");
    meta.addDependency("org.apache.solr", "solr-core", "7.2.1");
    meta.addDependency("org.apache.solr", "solr-solrj", "7.2.1");
    
    meta.addDependency("commons-io", "commons-io", "2.5");
    meta.addDependency("mrl-solr", "mrl-solr-data", "1.0", "zip");
    // Dependencies issue
    meta.setAvailable(true);
    return meta;
  }

  // Attach Pattern stuff! 
  public void attach(OpenCV opencv) {
    opencv.addListener("publishOpenCVData", getName(), "onOpenCVData");
    opencv.addListener("publishClassification", getName(), "onClassification");
    opencv.addListener("publishYoloClassification", getName(), "onYoloClassification");
      
  }

  public ArrayList<YoloDetectedObject> onYoloClassification (ArrayList<YoloDetectedObject> yoloObjects) {
    // for now.. let's just do this.
    for (YoloDetectedObject yolo : yoloObjects) {
      SolrInputDocument doc = new SolrInputDocument();
      String type = "yolo";    
      String id = type + "_" + UUID.randomUUID().toString();
      doc.setField("id", id);
      doc.setField("type", type);
      // TODO: enforce UTC, or move this to the solr schema to do.
      doc.setField("date", new Date());
      doc.addField("label", yolo.label);
      doc.addField("frame_index", yolo.frameIndex);
      doc.addField("confidence", yolo.confidence);
      // TODO something better.. also include the ROI from the original image.
      doc.addField("boundingbox", yolo.toString());
      // TODO: more meta data
      addDocument(doc);
    }
    // add the document we just built up to solr so we can remember it!   
    return yoloObjects;
  }
  
  public OpenCVData onOpenCVData(OpenCVData data) {
    // TODO: copy some useful metadata to the record being archived
    // TODO: convert this set of opencv data to a solr document...
    SolrInputDocument doc = new SolrInputDocument();
    // create a document id for this document 
    // TODO: make this something much more deterministic!! 
    String type = "opencvdata";	  
    String id = type + "_" + UUID.randomUUID().toString();
    doc.setField("id", id);
    doc.setField("type", type);
    // TODO: enforce UTC, or move this to the solr schema to do.
    doc.setField("date", new Date());
    doc.setField("frame_index", data.getFrameIndex());
    doc.setField("selected_filter_name", data.getSelectedFilterName());
    doc.setField("name", data.getName());
    // for now.. let's just do this.
    
    //ByteBuffer jpg = data.getJPGByteBuffer(data.getSelectedFilter());
    
//    byte[] bytes = jpg.array();
//    String encoded = Base64.encodeBase64String(bytes);
//    doc.addField("bytes", encoded);
    
    
//    for (String key : data.keySet()) {
//      IplImage img = data.get(key);
//      if (img == null) {
//        continue;
//      }
//     // byte[] bytes = new byte[img.imageSize()];
//      // img.asByteBuffer().get(bytes);
//      
//      //ByteBuffer buffer = img.asByteBuffer();
//      if (false) {
    
        // TODO: I might need to downsample this.. TODO: better downsampling.
        // assume frames per second is 30, sample once per second.
        if (data.getFrameIndex() % 30 == 0) {
          log.info("Saving snapshot..");
          Mat m = new Mat(data.getImage());
          long sz = (m.total() * m.channels());
          byte[] bytes = new byte[(int) sz];
          m.data().get(bytes);
          String encoded = Base64.encodeBase64String(bytes);
          doc.addField("bytes", encoded);
          // bytes field isn't searchaable
          doc.addField("has_bytes", true);
          log.warn("Image Size:" + encoded.length());
          m.close();
          
        }
//      }
//    }
    // add the document we just built up to solr so we can remember it!	  
    addDocument(doc);
    //  TODO: kw, why return anything here at all?! who would ever call this method and depend on the response?
    return data;
  }

  //
  public void attach(Deeplearning4j dl4j) {
	  dl4j.addListener("publishClassification", getName(), "onClassification");
  }
  
  // TODO: index the classifications with the cvdata. not separately.. 
  // o/w we need a way to relate back to the frame that this is a classification of
  public Map<String, Double> onClassification(Map<String, Double> data) throws SolrServerException, IOException {
    log.info("On Classification invoked!");
    SolrInputDocument doc = new SolrInputDocument();
    // create a document id for this document 
    // TODO: make this something much more deterministic!! 
    String type = "dl4j";	  
    String id = type + "_" + UUID.randomUUID().toString();
    doc.setField("id", id);
    doc.setField("type", type);
    // TODO: enforce UTC, or move this to the solr schema to do.
    doc.setField("date", new Date());
    // for now.. let's just do this.
    
    double threshold = 0.2;
   
    for (String key : data.keySet()) {
      double value = data.get(key);
      doc.addField(key, value);
      if (value > threshold) 
    	  doc.addField("object", key);
    //  doc.addField("recognized_object", key);
    }
    addDocument(doc);
    return data;
  }
  
  // TODO: pass the interface, not the specific service
  // i need more on the interface if we pass the itnerfae
  public void attach(WebkitSpeechRecognition recognizer) {
    recognizer.addTextListener(this);
  }
  public void attach(Sphinx recognizer) {
    recognizer.addTextListener(this);
  }

  @Override
  public void onText(String text) {
    // TODO Auto-generated method stub
    log.info("On Text (presumably from speech recognition) invoked!");
    SolrInputDocument doc = new SolrInputDocument();
    // create a document id for this document 
    // TODO: make this something much more deterministic!! 
    String type = "ear";   
    String id = type + "_" + UUID.randomUUID().toString();
    doc.setField("id", id);
    doc.setField("type", type);
    // TODO: enforce UTC, or move this to the solr schema to do.
    doc.setField("date", new Date());
    // for now.. let's just do this.
    doc.setField("text", text);
    // now we need to add the doc!
    addDocument(doc);
    
  }
  
  // TODO: align this with an interface, not an explicit service
  public void attach(ProgramAB programab) {
    programab.addResponseListener(this);
  }
  
  public void onResponse(Response response) {
    log.info("On Response invoked!");
    SolrInputDocument doc = new SolrInputDocument();
    // create a document id for this document 
    // TODO: make this something much more deterministic!! 
    String type = "programab";   
    String id = type + "_" + UUID.randomUUID().toString();
    doc.setField("id", id);
    doc.setField("type", type);
    // TODO: enforce UTC, or move this to the solr schema to do.
    doc.setField("date", new Date());
    // for now.. let's just do this.
    doc.setField("username", response.session);
    doc.setField("text", response.msg);
    // now we need to add the doc!
    addDocument(doc);
  }
 
  
  public void attachAllInboxes() {
    // attach all outboxes (except for our own..)
    for (ServiceInterface s : Runtime.getServices()) {
      if (s.getName().equalsIgnoreCase(this.getName())) 
        // attach all outboxes (except for our own..)
        continue;
      // TODO: avoid a double attach!  
      s.getInbox().addMessageListener(this);
    }
  }

  public void attachAllOutboxes() {
    // attach all outboxes (except for our own..)
    for (ServiceInterface s : Runtime.getServices()) {
      if (s.getName().equalsIgnoreCase(this.getName())) 
        // attach all outboxes (except for our own..)
        continue;
      // TODO: avoid a double attach!  
      s.getOutbox().addMessageListener(this);
    }
  }
  
  // TODO: attach a service inbox/outbox 
  // TODO: what does this mean for solr to attach to a service?
  public void attachInbox(Inbox inbox) {
    // TODO: refactor this to just be attach(Service)  or maybe we pass the inbox / outbox?
    inbox.addMessageListener(this);
  }
  

  public void attachOutbox(Outbox outbox) {
    // TODO: refactor this to just be attach(Service)  or maybe we pass the inbox / outbox?
    outbox.addMessageListener(this);
  }

  // TODO: see if we can figure out if this is an inbox or an outbox.
  // ok we want to do something like handle an onMessage method.
  public void onMessage(Message message) {
    if (message == null) { 
      // This shouldn't happen...
      log.warn("Null message in an inbox.. or maybe outbox?");
      return;
    }
    
    // convert this message into a solr document
    // TODO: make messages more unique. 
    String docId = "message_" + UUID.randomUUID().toString() + "_" +  message.msgId;
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    // TODO: consider a cache of this to make this faster
    doc.setField("sender_type", Runtime.getService(message.sender).getType());
    doc.setField("sender", message.sender);
    doc.setField("method", message.method);
    // TODO: this is actually the timestamp of the message.. not an id.
    doc.setField("message_id", message.msgId);
    doc.setField("message_type", message.msgType);
    doc.setField("message_name", message.name);
    doc.setField("sender_method", message.sendingMethod);
    doc.setField("message_status", message.status);
    if (message.historyList != null) {
      for (String history : message.historyList) {
        doc.addField("history", message.historyList);
      }
    }
    // System.out.println("Data: " + message.data);
    // TODO: now we need to introspect the array of objects and figure out how to index them!! gah..
    if (message.data != null) {
      for (Object o : message.data) {
        // TODO: this will probably blow up pretty bad for different object types
        doc.addField("data", o);
      }
    }
    addDocument(doc);
  }
}
