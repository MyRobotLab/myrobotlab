package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.SolrConfig;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
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
public class Solr extends Service<SolrConfig> implements DocumentListener, TextListener, MessageListener {


  private static final String CORE_NAME = "core1";
  public final static Logger log = LoggerFactory.getLogger(Solr.class);
  private static final long serialVersionUID = 1L;
  public String solrUrl = "http://localhost:8983/solr";
  transient private SolrClient solrServer;
  public boolean commitOnFlush = true;
  // the directory for the solr configs and index. (default to mrl/Solr)
  public String solrHome = "Solr";
  // EmbeddedSolrServer embeddedSolrServer = null;
  transient private EmbeddedSolrServer embeddedSolrServer = null;
  
  Collection<SolrInputDocument> documentBatch = Collections.synchronizedCollection(new ArrayList<>());
  // The batch size of documents to accumulate before flushing the batch to solr.
  public transient int batchSize = 100;
  // TODO: consider moving this tagging logic into opencv..
  // for now, we'll just set a counter that will count down how many opencv
  // frames
  // will be tagged with the given label.
  public String openCvLabel = null;
  public int openCvTrainingCount = 0;
  public int yoloPersonTrainingCount = 0;
  public String yoloPersonLabel = null;

  public Solr(String n, String id) {
    super(n, id);
  }

  public void startEmbedded() throws SolrServerException, IOException {
    startEmbedded(getDataInstanceDir());
  }

  /**
   * USE WITH CAUTION!!! This will DELETE ALL OF YOUR ROBOTS MEMORIES. THERE IS
   * NO RECOVERY FROM THIS.
   * 
   * @throws SolrServerException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public void deleteEmbeddedIndex() throws SolrServerException, IOException {
    if (embeddedSolrServer != null) {
      log.info("Deleting the entire index!!!!");
      embeddedSolrServer.deleteByQuery("*:*");
      embeddedSolrServer.commit();
      log.info("I know nothing...");
    } else {
      log.info("Only supported for embedded solr server");
    }
  }

  /**
   * Start the embedded Solr instance with the solr home directory provided.
   * This expects that you ahve a valid solr.xml and configset in that directory
   * named "core1"
   * 
   * @param path
   *          path to start for solr
   * @throws SolrServerException
   *           boom
   * @throws IOException
   *           boom
   */
  public synchronized void startEmbedded(String path) throws SolrServerException, IOException {
    // let's extract our default configs into the directory/
    // FileIO.extract(Util.getResourceDir() , "Solr/core1", path);
    // FileIO.extract(Util.getResourceDir() , "Solr/solr.xml", path +
    // File.separator + "solr.xml");
    // load up the solr core container and start solr
    if (embeddedSolrServer != null) {
      log.info("Embedded solr already running.");
      return;
    }
    
    // FIXME - a bit unsatisfactory
    File f = new File(getDataInstanceDir());
    f.mkdirs();

    File check = new File(FileIO.gluePaths(path, "core1"));
    if (!check.exists()) {
      FileIO.copy(getResourceDirList(), path);
    }
    Path solrHome = Paths.get(path);
    log.info(solrHome.toFile().getAbsolutePath());
    Path solrXml = solrHome.resolve("solr.xml");

    String absolueHome = solrHome.toFile().getAbsolutePath();
    CoreContainer cores = CoreContainer.createAndLoad(Paths.get(absolueHome), solrXml);
    for (String coreName : cores.getAllCoreNames()) {
      log.info("Found core core {}", coreName);
    }

    // create the actual solr instance with core1
    embeddedSolrServer = new EmbeddedSolrServer(cores, CORE_NAME);
    // TODO: verify when the embedded solr server has fully started.
  }

  /**
   * Add a single document at a time to the solr server.
   * 
   * @param doc
   *          the input doc to send to solr (prefer to send batches with addDocuments instead)
   *   
   */
  public void addDocument(SolrInputDocument doc) {
    // Always batch!
    addDocuments(List.of(doc));
  }

  /**
   * Add a batch of documents (this is more efficient than adding one at a time.)
   * 
   * @param docs
   *          a collection of solr input docs to add to solr.
   */
  public void addDocuments(Collection<SolrInputDocument> docs) {
    // TODO: setup a thread to flush this batch
    // performance optimization.. always batch updates to solr.
    documentBatch.addAll(docs);
    flushDocumentBatch(false);
  }

  private ProcessingStatus flushDocumentBatch(boolean forceFlush) {
    synchronized (documentBatch) {
      if (documentBatch.size() >= batchSize || forceFlush) {
        try {
          if (embeddedSolrServer != null) {
            embeddedSolrServer.add(documentBatch);
          } else {
            solrServer.add(documentBatch);
          }
          // we sent the batch, so let's clear it up.
          documentBatch.clear();
        } catch (SolrServerException e) {
          log.warn("An exception occurred when trying to add documents to the index.", e);
          // ??
          return ProcessingStatus.ERROR;
        } catch (IOException e) {
          log.warn("A network exception occurred when trying to add documents to the index.", e);
          return ProcessingStatus.ERROR;
        }
      }
    }
    return ProcessingStatus.OK;
  }

  /**
   * Commit the solr index and make documents that have been submitted become
   * searchable. There is also a timed "autoCommit" setting in the
   * solrconfig.xml
   * 
   */
  public void commit() {
    // if we are explicitly calling a commit.. first flush any partial batch
    // followed by the commit.
    flushDocumentBatch(true);
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

  /**
   * Delete a single document from the index provided a specific doc id.
   * 
   * @param docId
   *          id of the solr document to delete
   * 
   */
  public void deleteDocument(String docId) {
    try {
      if (embeddedSolrServer != null) {
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
   * Returns a document given the doc id from the index if it exists otherwise
   *
   * @param docId
   *          - the doc id
   * @return - the solor document
   */
  public SolrDocument getDocById(String docId) {
    SolrQuery query = new SolrQuery();
    query.set("q", "id:\"" + docId + "\"");
    query.setRows(1);
    QueryResponse resp = null;
    if (embeddedSolrServer != null) {
      try {
        resp = embeddedSolrServer.query(query);
      } catch (SolrServerException | IOException e) {
        log.warn("Exception running embedded solr search : {}", query, e);
      }
    } else {
      try {
        resp = solrServer.query(query);
      } catch (SolrServerException | IOException e) {
        // TODO Auto-generated catch block
        log.warn("Exception running solr search : {}", query, e);
      }
    }
    long num = resp.getResults().getNumFound();
    if (num == 0) {
      // TODO: log a message that the doc wasn't found or something
      return null;
    }
    return resp.getResults().get(0);
  }

  /**
   * @return The url for the solr instance you wish to query. Defaults to
   *         http://localhost:8983/solr
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
      log.warn("An error occurred when optimizing the index.", e);
    } catch (IOException e) {
      log.warn("A network error occurred when optimizing the index, solr down?", e);
    }
  }

  /**
   * Pass in custom solr query parameters and execute that query.
   * 
   * @param query
   *          the query to execute
   * @return a query response from solr
   */
  public QueryResponse search(SolrQuery query) {
    log.info("Solr Query Request: {}", query);
    QueryResponse resp = null;
    try {
      if (embeddedSolrServer != null) {
        resp = embeddedSolrServer.query(query);
      } else {
        resp = solrServer.query(query);
      }
    } catch (SolrServerException | IOException e) {
      log.warn("Exception running search {}", query, e);
    }
    return resp;
  }

  /**
   * Helper method that will run a search, and return the bytes field from the
   * first result decoded into an IplImage
   * 
   * @param queryString
   *          query to find the image
   * @return an ipl image from the index
   * @throws IOException
   *           if in error
   */
  public IplImage fetchImage(String queryString) throws IOException {
    String fieldName = "bytes";
    // return an IplImage from the solr index.!
    QueryResponse qr = search(queryString, 1, 0, false);
    if (qr.getResults().getNumFound() > 0) {
      Object result = qr.getResults().get(0).getFirstValue(fieldName);
      // TODO: this is a byte array or is it base64?
      // byte[] decoded = Base64.decodeBase64((byte[])result);
      // read these bytes as an image.
      IplImage image = Util.bytesToImage((byte[]) result);
      String docId = qr.getResults().get(0).getFirstValue("id").toString();
      // show(image, docId);
      return image;
    } else {
      log.info("Not Found");
      return null;
    }
  }

  /**
   * This query returns the superset of all data that will be used for training
   * and testing of a dl4j model. This will return a query that when executed
   * will return the number of records found for the query, as well as a facet
   * on the label field.
   * 
   * @param queryString
   *          the query string
   * @param labelField
   *          the field containing the labels
   * @return a query request
   * 
   */
  public SolrQuery makeDatasetQuery(String queryString, String labelField) {
    SolrQuery solrQuery = new SolrQuery(queryString);
    // TODO: avoid this and use cursor mark pagination.
    solrQuery.setRows(0);
    // add a facet on the label field so we know what's in the training dataset.
    solrQuery.addFacetField(labelField);
    solrQuery.setFacetMinCount(1);
    solrQuery.setFacet(true);
    return solrQuery;
  }

  public void createTrainingDataDir(SolrQuery query, String directory) throws IOException {
    // This method will iterate a result set that contains images stored in the
    // "bytes" field of a document
    // It will then save these images to a directory based on the "label" field.
    // TODO: use cursor mark for deep pagination to produce this training set
    // and avoid large memory usage
    QueryResponse qres = search(query);
    File trainingDir = new File(directory);
    if (!trainingDir.exists()) {
      trainingDir.mkdirs();
    }
    // Ok directory exists.. iterate results and save bytes
    for (SolrDocument doc : qres.getResults()) {
      // TODO: bunch of null pointer checks here..
      String id = doc.getFirstValue("id").toString();
      byte[] bytes = (byte[]) doc.getFirstValue("bytes");
      String label = (String) doc.getFirstValue("label");

      String labelDir = directory + File.separator + label;
      if (!new File(labelDir).exists()) {
        new File(labelDir).mkdirs();
      }
      // TODO: scrub the label field to make it directory name safe.
      // TODO: i'm pretty sure i save them as png bytes
      String targetFile = labelDir + File.separator + id + ".png";
      log.info("Saving {}", targetFile);
      FileOutputStream stream = new FileOutputStream(targetFile);
      try {
        stream.write(bytes);
      } finally {
        stream.close();
      }
    }
  }

  /**
   * Helper search function that runs a search and returns a specified field
   * from the first result
   * 
   * @param queryString
   *          query string
   * @param fieldName
   *          field name
   * @return the value from the field
   * 
   */
  public String fetchFirstResultField(String queryString, String fieldName) {
    QueryResponse qr = search(queryString, 1, 0, false);
    if (qr.getResults().getNumFound() > 0) {
      Object result = qr.getResults().get(0).getFirstValue(fieldName);
      if (result == null) {
        return "not found";
      } else {
        return (String) result;
      }
    } else {
      return "not found";
    }
  }

  public String fetchFirstResultSentence(String queryString, String fieldName) {
    String res = fetchFirstResultField(queryString, fieldName);
    // Now we want to sentence detect this string.. and return the first
    // sentence..
    // for now.. cheating, and just pulling everything up to the first period.
    if (!StringUtils.isEmpty(res)) {
      // TODO: better sentence boundary detection
      String fragment = res.split("\\.")[0];
      return fragment;
    } else {
      // TODO: log a warning or something.
      return null;
    }
  }

  /**
   * Default query to fetch the top 10 documents that match the query request.
   * 
   * @param queryString
   *          the query string
   * @return a query response
   */
  public QueryResponse search(String queryString) {
    // default to 10 hits returned.
    // System.err.println("Here...");
    return search(queryString, 10, 0, true);
  }

  /**
   * Default query to fetch the top 10 documents that match the query request.
   * 
   * @param queryString
   *          query string
   * @param rows
   *          number of rows to return
   * @param start
   *          offset into the restult
   * @param mostRecent
   *          specify index_date sort
   * @return the response
   */
  public QueryResponse search(String queryString, int rows, int start, boolean mostRecent) {
    log.info("Searching for : {}", queryString);
    SolrQuery query = new SolrQuery();
    query.set("q", queryString);
    query.setRows(rows);
    query.setStart(start);
    if (mostRecent)
      query.setSort(new SortClause("index_date", ORDER.desc));
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


  /**
   * Default query to fetch the top 10 documents that match the query request.
   * 
   * @param queryString
   *          query string
   * @param rows
   *          number of rows to return
   * @param start
   *          offset into the restult
   * @param facetFields
   *          a list of fields in which to return facets for
   * @return the response
   */
  public QueryResponse searchWithFacets(String queryString, int rows, int start, String[] facetFields, String[] filters) {
    log.info("Searching for (with facets): {}", queryString);
    int numFacetBuckets = 50;
    SolrQuery query = new SolrQuery();
    query.set("q", queryString);
    query.setRows(rows);
    query.setStart(start);
    query.setFacet(true);
    query.setFacetLimit(numFacetBuckets);
    query.setFacetMinCount(1);
    // default search fields.
    query.add("qf", "title^10");
    query.add("qf", "artist^5");
    query.add("qf", "album^2");
    query.add("qf", "genre");
    query.add("qf", "year");

    query.setParam("defType", "edismax");
    query.setParam("q.op", "AND");
    // TODO: expose sorting in a fancier search method signature
    // Alternatively, pass the list of parameters and their values into a generic search method instead.
    query.setSort("index_date", ORDER.desc);
    for (String field : facetFields) {
      query.addFacetField(field);
    }
    for (String filter : (String[])filters) {
      query.addFilterQuery(filter);      
    }

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

  public String publishResults(QueryResponse resp) {
    // The QueryResponse object doesn't properly serialize some useful info
    // from the results  ( SolrDocumentList ) object, like the number found, the start offset
    // So we manually copy that info to top level items in the response so the webgui
    // can get at it.
    // TODO: why are the facet buckets not ordered!!!
    long numFound = resp.getResults().getNumFound();
    long start = resp.getResults().getStart();
    Float maxScore = resp.getResults().getMaxScore();
    Boolean numFoundExact = resp.getResults().getNumFoundExact();
    // this is lame but we have to copy some metadata around on the response
    // because it doesn't serialize properly.
    resp.getResponse().add("numFound", numFound);
    resp.getResponse().add("start", start);
    resp.getResponse().add("maxScore", maxScore);
    resp.getResponse().add("numFoundExact", numFoundExact);
    resp.getResponse().add("size", resp.getResults().size());
    // Now, let's convert the byte arrays to base64 image strings.
    for (SolrDocument d : resp.getResults()) {
      // TODO: decide what to do based on the mime type...
      if (d.containsKey("bytes")) {
        // encode this as base 64 image data.
        // TODO: support multiple byte arrays.
        byte[] bytes = (byte[])(d.getFirstValue("bytes"));
        String base64jpg = Util.bytesToBase64Jpg(bytes);
        d.setField("bytes", base64jpg);
      }
    }

    String jsonResponse = resp.jsonStr(); 
    // publish the json string of the response.
    // System.err.println(jsonResponse);
    
    return jsonResponse;
  };

  /**
   * Set the url for the solr instance to communicate with. This is not used
   * with the embedded solr server
   * 
   * @param solrUrl
   *          the solr url to connect to. (HttpSolrClient)
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
  // TODO: why do we return ProcessingStatus here?! 
  public ProcessingStatus onDocuments(List<Document> docs) {
    // Convert the input document to a solr input docs and send it!
    if (docs.size() == 0) {
      log.warn("Empty list of documents received.");
      return ProcessingStatus.OK;
    }
    ArrayList<SolrInputDocument> docsToSend = new ArrayList<SolrInputDocument>();
    for (Document d : docs) {
      documentBatch.add(convertDocument(d));
    }
    return flushDocumentBatch(false);
  }

  private SolrInputDocument convertDocument(Document doc) {
    SolrInputDocument solrDoc = new SolrInputDocument();
    solrDoc.setField("id", doc.getId());
    for (String fieldName : doc.getFields()) {
      for (Object o : doc.getField(fieldName)) {
        if (o != null) {
          solrDoc.addField(fieldName, o);
          // let's implicitly add a text_en version of the field.
          // TODO: understand language detection on the field and dynamically
          // specficy which field type to use.
          solrDoc.addField(fieldName + "_txt_en", o);
        }
      }
    }
    return solrDoc;
  }

  @Override
  public ProcessingStatus onDocument(Document doc) {
    // always be batching when sending docs.
    // TODO: we want to add to the current batch to send..  
    // and make sure we have a thread flushing the batch if it gets too old.
    return onDocuments(List.of(doc));
  }

  @Override
  public boolean onFlush() {
    // if we got a flush call, let's flush any partial batch.
    flushDocumentBatch(true);
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

  // Attach Pattern stuff!
  public void attach(OpenCV opencv) {
    opencv.addListener("publishOpenCVData", getName());
    opencv.addListener("publishClassification", getName());
    opencv.addListener("publishYoloClassification", getName());
  }

  // to make it easier to call from aiml
  public void setYoloPersonTrainingLabel(String label, String count) {
    setYoloPersonTrainingLabel(label, Integer.valueOf(count));
  }

  // sets it so the next N opencv frames will be tagged with the training label.
  public void setYoloPersonTrainingLabel(String label, int count) {
    this.yoloPersonLabel = label;
    this.yoloPersonTrainingCount = count;
  }

  // what to index when a yolo event occurs
  public ArrayList<YoloDetectedObject> onYoloClassification(ArrayList<YoloDetectedObject> yoloObjects) {

    if (yoloPersonTrainingCount <= 0) {
      // skip it.. we're not recording
      return yoloObjects;
    }

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
      // If there is training information about the current type of object ...
      // TODO:
      if (yolo.label.equalsIgnoreCase("person")) {
        // Here we should also add the label.
        // we are training.
        if (yoloPersonTrainingCount >= 0) {
          // decrement the count
          yoloPersonTrainingCount--;
          if (yoloPersonTrainingCount > 0) {
            invoke("publishDoneYoloLabel", yoloPersonLabel);
          }
          doc.setField("person_label", yoloPersonLabel);
          byte[] bytes = null;
          try {
            bytes = Util.imageToBytes(yolo.image);
            String encoded = CodecUtils.toBase64(bytes);
            // bytes field contains bindary data (sent as base64)
            doc.addField("bytes", encoded);
            doc.addField("has_bytes", true);
            log.info("Image Size:{}", encoded.length());
          } catch (IOException e) {
            log.warn("Error creating bytes field.", e);
            continue;
          }
        }
      }
      addDocument(doc);
    }
    // add the document we just built up to solr so we can remember it!
    return yoloObjects;
  }

  // subscribe to this to get a callback when a particular yolo person label
  // finished
  public String publishDoneYoloLabel(String label) {
    // here we should publish
    return label;
  }

  // subscribe to this to get the vgg16 dl4j transfer learning labeling is done.
  public String publishDoneLabeling(String label) {
    return label;
  }

  // to make it easier to call from aiml
  public void setTrainingLabel(String label, String count) {
    setTrainingLabel(label, Integer.valueOf(count));
  }

  // sets it so the next N opencv frames will be tagged with the training label.
  public void setTrainingLabel(String label, int count) {
    this.openCvLabel = label;
    this.openCvTrainingCount = count;
  }

  // when attached to an opencv instance this will return images and save them
  // to solr if there is a label / count specified
  public OpenCVData onOpenCVData(OpenCVData data) {
    // System.err.println("Open CV Data..");
    // Only record if we are training.
    // TODO: unroll this use case.. we'd like to just blindly index all the frames here.
    if (false) {
      if (openCvLabel == null) {
        // we're not training just return
        return data;
      }
      // we are training.
      if (openCvTrainingCount == 0) {
        // we're done recording our training data for this one.
        openCvLabel = null;
        return data;
      } else {
        // decrement the count
        openCvTrainingCount--;
        if (openCvTrainingCount == 0) {
          invoke("publishDoneLabeling", openCvLabel);
        }
      }
    }
    // ok.. here we are, create a "memory" out of the opencv data.
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
    doc.setField("selected_filter_name", data.getSelectedFilter());
    doc.setField("name", data.getName());
    // add the training label
    doc.setField("label", openCvLabel);
    IplImage img = data.getImage();
    byte[] bytes = null;
    try {
      // Let's compress the raw image.. o/w it'll just likely be much too large.
      bytes = Util.imageToBytes(img);
      String encoded = CodecUtils.toBase64(bytes);
      // bytes field contains binary data (sent as base64)
      doc.addField("bytes", encoded);
      doc.addField("has_bytes", true);
      log.warn("Image Size:{}", encoded.length());
    } catch (IOException e) {
      log.warn("Exception Storing Image in Solr.", e);
      return data;
    }
    // add the document we just built up to solr so we can remember it!
    log.info("Saving snapshot.. of {}.", openCvLabel);
    addDocument(doc);
    // TODO: kw, why return anything here at all?! who would ever call this
    // method and depend on the response?
    return data;
  }

  // attach pattern stuff
  public void attach(Deeplearning4j dl4j) {
    dl4j.addListener("publishClassification", getName());
  }

  // TODO: index the classifications with the cvdata. not separately..
  // o/w we need a way to relate back to the frame that this is a classification
  // of
  public Map<String, Double> onClassification(Map<String, Double> data) throws SolrServerException, IOException {
    // log.info("On Classification invoked!");
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
      // doc.addField("recognized_object", key);
    }
    addDocument(doc);
    return data;
  }

  // TODO: pass the interface, not the specific service
  // i need more on the interface if we pass the itnerfae
  public void attach(WebkitSpeechRecognition recognizer) {
    recognizer.addTextListener(this);
  }

  public void attach(SpeechRecognizer recognizer) {
    recognizer.attachTextListener(this);
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
    doc.setField("username", response.userName);
    doc.setField("text", response.msg);
    // now we need to add the doc!
    addDocument(doc);
  }

  /**
   * This method will iterate though all services in the system (except itself.)
   * and attach the inboxes for indexing. Any time something is added to the
   * inbox of a service, it will also trigger that message getting indexed in
   * solr
   * 
   * this method can generate huge indexes and a lot of useless data!
   */
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

  /**
   * This method will iterate though all services in the system (except itself.)
   * and attach the outboxes for indexing. Any time something is added to the
   * outbox of a service, it will also trigger that message getting indexed in
   * solr. This happens when an invoke is called in the framework
   * 
   * this method can generate huge indexes and a lot of useless data!
   */

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

  // attach a specific inbox
  public void attachInbox(Inbox inbox) {
    // TODO: refactor this to just be attach(Service) or maybe we pass the inbox
    // / outbox?
    inbox.addMessageListener(this);
  }

  // attach a specific outbox
  public void attachOutbox(Outbox outbox) {
    // TODO: refactor this to just be attach(Service) or maybe we pass the inbox
    // / outbox?
    outbox.addMessageListener(this);
  }

  // TODO: see if we can figure out if this is an inbox or an outbox.
  // ok we want to do something like handle an onMessage method.
  @Override
  public void onMessage(Message message) {
    if (message == null) {
      // This shouldn't happen...
      log.warn("Null message in an inbox.. or maybe outbox?");
      return;
    }
    // convert this message into a solr document
    // TODO: make messages more unique.
    String docId = "message_" + UUID.randomUUID().toString() + "_" + message.msgId;
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", docId);
    // TODO: consider a cache of this to make this faster
    doc.setField("type", "message");
    doc.setField("sender_type", Runtime.getService(message.sender).getTypeKey());
    doc.setField("sender", message.sender);
    doc.setField("method", message.method);
    // TODO: this is actually the timestamp of the message.. not an id.
    doc.setField("message_id", message.msgId);
    doc.setField("message_dataEncoding", message.encoding);
    doc.setField("message_name", message.getName());
    doc.setField("sender_method", message.sendingMethod);
    doc.setField("message_status", message.status);
    /*
     * This makes no sense.. if (message.getHops() != null) { for (String
     * history : message.getHops()) { doc.addField("history",
     * message.getHops()); } }
     */

    // System.out.println("Data: " + message.data);
    // TODO: now we need to introspect the array of objects and figure out how
    // to index them!! gah..
    if (message.data != null) {
      for (Object o : message.data) {
        // TODO: this will probably blow up pretty bad for different object
        // types
        doc.addField("data", o);
      }
    }
    addDocument(doc);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Python python = (Python) Runtime.start("python", "Python");
      Solr solr = (Solr) Runtime.start("solr", "Solr");
      solr.startEmbedded();
      WebGui webgui = (WebGui)Runtime.start("webgui", "WebGui");
      OpenCV cv = (OpenCV)Runtime.start("cv", "OpenCV");

      Runtime.start("programab", "ProgramAB");
      Runtime.start("clock", "Clock");
      Runtime.start("ard", "Arduino");
      
      solr.attachAllInboxes();
      solr.attachAllOutboxes();
      // lets start indexing frames!
      solr.attach(cv);
      // and, go!
      //cv.capture();

      // Create a test document
      boolean testIndex = false;
      if (testIndex) {
        SolrInputDocument doc = new SolrInputDocument();
        /*
         * doc.setField("id", "Doc1"); doc.setField("title", "My title");
         * doc.setField("content",
         * "This is the text field, for a sample document in myrobotlab.  "); //
         * add the document to the index solr.addDocument(doc); // commit the
         * index solr.commit();
         */
        doc = new SolrInputDocument();
        doc.setField("id", "Doc3");
        doc.setField("title", "My title 3");
        doc.setField("content", "This is the text field, for a sample document in myrobotlab. 2 ");
        doc.setField("annoyance", 1);
        // add the document to the index
        solr.addDocument(doc);
        // commit the index
        solr.commit();

        // search for the word myrobotlab
        String queryString = "content:myrobotlab";
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
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * This method will issue an atomic update to the solr index for a given
   * document id the value will be set on the document
   * 
   * @param docId
   *          doc id (required)
   * @param fieldName
   *          field name to update
   * @param value
   *          new value for field
   * @throws SolrServerException
   *           boom
   * @throws IOException
   *           boom
   * 
   */
  public void updateDocument(String docId, String fieldName, String value) throws SolrServerException, IOException {
    SolrInputDocument sdoc = new SolrInputDocument();
    sdoc.addField("id", docId);
    Map<String, Object> fieldModifier = new HashMap<>(1);
    fieldModifier.put("set", value);
    sdoc.addField(fieldName, fieldModifier); // add the map as the field value
    if (embeddedSolrServer != null) {
      // use this/
      embeddedSolrServer.add(sdoc);
    } else {
      // default solr client.
      solrServer.add(sdoc);
    }
  }

  public void shutdown() {
    //
    if (embeddedSolrServer != null) {
      try {
        embeddedSolrServer.close();
      } catch (IOException e) {
        log.warn("Exception shutting down the embedded solr server.", e);
      }
    }
    if (solrServer != null) {
      try {
        solrServer.close();
      } catch (IOException e) {
        log.warn("Exception disconnecting from remote Solr server.", e);
      }
    }

  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  @Override
  public void releaseService() {
    shutdown();
    super.releaseService();
  }

  @Override
  public SolrConfig apply(SolrConfig inConfig) {
    // 
    super.apply(inConfig);
    if (config.embedded) {
      // 
      try {
        startEmbedded();
      } catch (SolrServerException | IOException e) {
        // TODO: how should we handle this?
        log.warn("Error starting embedded solr instance.", e);
        e.printStackTrace();
      };
    }
    return config;
  }

  @Override
  public SolrConfig getConfig() {
    super.getConfig();

    if (this.embeddedSolrServer != null) {
      config.embedded = true;
    }
    
    if (this.solrUrl != null) {
      config.solrUrl = this.solrUrl;
    }
    
    return config;
  }
  
}
