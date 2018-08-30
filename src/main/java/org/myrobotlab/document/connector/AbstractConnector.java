package org.myrobotlab.document.connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.DocumentConnector;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;

/**
 * 
 * AbstractConnector - base class for implementing a new document connector
 * service.
 * 
 */
public abstract class AbstractConnector extends Service implements DocumentPublisher, DocumentConnector {

  private static final long serialVersionUID = 1L;

  protected ConnectorState state = ConnectorState.STOPPED;
  private int batchSize = 1;
  private List<Document> batch = Collections.synchronizedList(new ArrayList<Document>());

  private String docIdPrefix = "";
  private Integer feedCount = 0;
  private long lastUpdate = System.currentTimeMillis();
  private long start = System.currentTimeMillis();
  
  public AbstractConnector(String name) {
    super(name);
    // no overruns!
    this.getOutbox().setBlocking(true);
  }

  public abstract void setConfig(ConnectorConfig config);

  public void feed(Document doc) {
    

    // System.out.println("Feeding document " + doc.getId());
    // TODO: add batching and change this to publishDocuments (as a list)
    // Batching for this sort of stuff is a very good thing.
    if (batchSize <= 1) {
      feedCount++;
      invoke("publishDocument", doc);
    } else {
      // handle the batch
      // TODO: make this synchronized and thread safe!
      batch.add(doc);
      if (batch.size() >= batchSize) {
        feedCount += batch.size();
        flush();
      }
    }
    
    // update and report timing metrics
    long now = System.currentTimeMillis();
    long lastReport = now - lastUpdate;
    // every 10 seconds
    if (lastReport > 10000) {
      // log the throughput
      double speed = feedCount / (double)(now - start) * 1000;
      log.info("Feed {} docs.  Current rate {}", feedCount, speed);
      lastUpdate = now;
    }
    
  }

  public void publishFlush() {
    // NoOp
    // Here for the framework to invoke it on the down stream services.
  };

  public void flush() {
    // flush any partial batch
    // TODO: make this thread safe!
    invoke("publishDocuments", batch);
    // invoke("publishFlush");
    // reset/clear the batch.
    batch = new ArrayList<Document>();
    // TODO: I worry there's a race condition here.. but maybe not... more
    // testing will show.
    while (getOutbox().size() > 0 && !state.equals(ConnectorState.RUNNING)) {
      // TODO: wait until the outbox is empty... perhaps we also need to
      // validate if we've been interrupted?
      log.info("Draining out box Size: {} Connector State: {}", getOutbox().size(), state);
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      continue;
    }
  }

  public ConnectorState getState() {
    return state;
  }

  public void setState(ConnectorState state) {
    this.state = state;
  }

  public Document publishDocument(Document doc) {
    return doc;
  }

  public List<Document> publishDocuments(List<Document> batch) {
    return batch;
  }

  public void addDocumentListener(DocumentListener listener) {
    addListener("publishDocument", listener.getName(), "onDocument");
    addListener("publishDocuments", listener.getName(), "onDocuments");
    addListener("publishFlush", listener.getName(), "onFlush");
  }

  public ConnectorState getConnectorState() {
    return state;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public String getDocIdPrefix() {
    return docIdPrefix;
  }

  public void setDocIdPrefix(String docIdPrefix) {
    this.docIdPrefix = docIdPrefix;
  }

}
