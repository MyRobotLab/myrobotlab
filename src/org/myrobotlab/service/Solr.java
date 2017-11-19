package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DocumentListener;
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
public class Solr extends Service implements DocumentListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Solr.class);

  public String solrUrl = "http://localhost:8983/solr";

  transient private HttpSolrServer solrServer;

  public boolean commitOnFlush = true;

  /*
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   */

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      Solr solr = (Solr) Runtime.start("solr", "Solr");
      Runtime.start("gui", "SwingGui");
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

      solrServer.add(doc);
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
      solrServer.add(docs);
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
      solrServer.commit();
    } catch (SolrServerException e) {
      log.warn("An exception occurred when trying to commit the index.", e);
    } catch (IOException e) {
      log.warn("A network exception occurred when trying to commit the index.", e);
    }
  }

  public void deleteDocument(String docId) {
    try {
      solrServer.deleteById(docId);
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
      resp = solrServer.query(query);
    } catch (SolrServerException e) {
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
      resp = solrServer.query(query);
    } catch (SolrServerException e) {
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
      solrServer = new HttpSolrServer(solrUrl);
    }
  }

  @Override
  public void startService() {
    super.startService();
    solrServer = new HttpSolrServer(solrUrl);
  }

  @Override
  public ProcessingStatus onDocuments(List<Document> docs) {
    // Convert the input document to a solr input docs and send it!
    ArrayList<SolrInputDocument> docsToSend = new ArrayList<SolrInputDocument>();
    for (Document d : docs) {
      docsToSend.add(convertDocument(d));
    }
    try {
      solrServer.add(docsToSend);
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
        solrDoc.addField(fieldName, o);
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
    meta.addDependency("org.apache.solr", "4.10.2");
    // Dependencies issue
    meta.setAvailable(false);
    return meta;
  }

}
