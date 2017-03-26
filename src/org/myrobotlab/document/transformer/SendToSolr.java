package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This stage will convert an MRL document to a solr document. It then batches
 * those documents and sends the batches to solr. Upon a flush call any partial
 * batches will be flushed.
 * 
 * @author kwatters
 *
 */
public class SendToSolr extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(SendToSolr.class);
  private String idField = "id";
  private String fieldsField = "fields";
  private boolean addFieldsField = false;
  private SolrServer solrServer = null;
  private String solrUrl = "http://localhost:8983/solr/collection1";
  private boolean issueCommit = true;

  private int batchSize = 100;
  // private LinkedBlockingQueue<SolrInputDocument> batch = new
  // LinkedBlockingQueue<SolrInputDocument>();
  // Synchronized list. needed for thread safety.
  private List<SolrInputDocument> batch = Collections.synchronizedList(new ArrayList<SolrInputDocument>());

  // private String basicAuthUser = null;
  // private String basicAuthPass = null;

  // Batch size +/-

  @Override
  public void startStage(StageConfiguration config) {
    solrUrl = config.getProperty("solrUrl", solrUrl);
    issueCommit = config.getBoolParam("issueCommit", new Boolean(issueCommit));
    batchSize = Integer.valueOf(config.getIntegerParam("batchSize", batchSize));

    // basicAuthUser = config.getStringParam("basicAuthUser", basicAuthUser);
    // basicAuthPass = config.getStringParam("basicAuthPass", basicAuthPass);

    // Initialize a connection to the solr server on startup.
    if (solrServer == null) {
      // TODO: support an embeded solr instance
      log.info("Connecting to Solr at {}", solrUrl);
      // set credentials.

      // if (basicAuthUser != null) {
      // DefaultHttpClient httpClient = new DefaultHttpClient();
      // httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, new
      // UsernamePasswordCredentials(basicAuthUser, basicAuthPass));
      // create solr server with client.
      // solrServer = new HttpSolrServer( solrUrl , httpClient);
      // } else {
      solrServer = new HttpSolrServer(solrUrl);
      // }
    } else {
      log.info("Solr instance already created.");
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    SolrInputDocument solrDoc = new SolrInputDocument();

    // set the id field on the solr doc
    String docId = doc.getId();
    for (String fieldName : doc.getFields()) {
      for (Object value : doc.getField(fieldName)) {
        solrDoc.addField(fieldName, value);
      }
      if (addFieldsField) {
        solrDoc.addField(fieldsField, fieldName);
      }
    }
    // prevent id field duplicate values.
    // remove the id field if it was set,
    solrDoc.removeField(idField);
    // make sure we add it back
    solrDoc.setField(idField, docId);
    // I guess we have the full document, we should send it
    // ArrayList<SolrInputDocument> solrDocs = new
    // ArrayList<SolrInputDocument>();
    // solrDocs.add(solrDoc);
    try {
      synchronized (batch) {
        batch.add(solrDoc);
        if (batch.size() >= batchSize) {
          // System.out.println("Solr Server Flush Batch...");
          // you are blocking?
          solrServer.add(batch);
          log.info("Sending Batch to Solr. Size: {}", batch.size());
          // System.out.println("Solr batch sent..");
          // batch.clear();
          batch = Collections.synchronizedList(new ArrayList<SolrInputDocument>());
        } else {
          // System.out.println("Batch Size " + batch.size());
        }
      }
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // TODO: NO COMMITS HERE!
    // solrServer.commit();
    return null;

  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub
    flush();

  }

  public synchronized void flush() {

    // Is this where I should flush the last batch?
    if (solrServer != null && batch.size() > 0) {
      try {
        log.info("flushing last batch. Size: {}", batch.size());
        solrServer.add(batch);
      } catch (SolrServerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        batch.clear();
      }
    }

    // TODO: should we commit on flush?
    try {
      if (issueCommit) {
        log.info("Committing solr");
        solrServer.commit();
      }
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // super.flush();

  }
}