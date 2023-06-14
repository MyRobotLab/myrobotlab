package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.document.workflow.WorkflowMessage;
import org.myrobotlab.document.workflow.WorkflowServer;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;

public class DocumentPipeline extends Service implements DocumentListener, DocumentPublisher {

  private static final long serialVersionUID = 1L;

  private WorkflowConfiguration workFlowConfig;
  private transient WorkflowServer workflowServer;
  private String workflowName = "default";

  public DocumentPipeline(String n, String id) {
    super(n, id);
  }

  static public String[] getCategories() {
    // TODO Auto-generated method stub
    return new String[] { "data" };
  }

  public void setConfig(WorkflowConfiguration workflowConfig) {
    this.workFlowConfig = workflowConfig;
  }

  @Override
  public Document publishDocument(Document doc) {
    // publish the document to the framework
    return doc;
  }

  @Override
  public ProcessingStatus onDocument(Document doc) {
    // TODO Auto-generated method stub
    // process the document! return a processing status!

    WorkflowMessage msg = new WorkflowMessage();
    msg.setDoc(doc);
    // for now only default workflow supported (1 workflow per DocumentPipeline
    // service i guess?)
    msg.setWorkflow(workflowName);
    // TODO: the type message should be add/update/delete sort of message types.
    // msg.setType(type);
    try {
      workflowServer.processMessage(msg);
    } catch (InterruptedException e) {
      e.printStackTrace();
      // TODO: this isn't correct!
      return ProcessingStatus.ERROR;
    }

    // TODO: we need to properly track the status of the message we just sent
    // off..
    // callbacks should be re-designed here .. this processing status is kinda
    // not correct.
    return ProcessingStatus.OK;
  }

  public void flush() {

    while (getInbox().size() > 0) {
      // TODO: we've gotta wait until we've consumed our inbox if we're
      // flushing?
      // TODO: This seems dangerous if we want to flush while continously
      // feeding
      // we'll never get to flush unless data pauses while we catchup.
      try {
        log.info("Waiting for inbox to drain...Size: {}", getInbox().size());
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    log.info("Inbox size is {}", getInbox().size());
    log.info("outbox size is {}", getOutbox().size());
    workflowServer.flush(workflowName);
    // TODO: what if our inbox isn't empty?

  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);
    
    WebGui webgui = (WebGui)Runtime.start("webgui", "WebGui");

    // start embedded solr
    Solr solr = (Solr)Runtime.start("solr","Solr");
    solr.startEmbedded();
    
    // start the pipeline to process the files from the file system
    DocumentPipeline pipeline = (DocumentPipeline) Runtime.start("docproc", "DocumentPipeline");
    // pipeline.workflowName = "default";
    // create a workflow to load into that pipeline service
    WorkflowConfiguration workflowConfig = new WorkflowConfiguration("default");
    workflowConfig.setName("default");
    workflowConfig.setNumWorkerThreads(8);
    
    StageConfiguration stage1Config = new StageConfiguration();
    stage1Config.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
    stage1Config.setStageName("SetTableField");
    stage1Config.setStringParam("type", "file");
    workflowConfig.addStage(stage1Config);

    StageConfiguration stage2Config = new StageConfiguration();
    stage2Config.setStageClass("org.myrobotlab.document.transformer.TextExtractor");
    stage2Config.setStageName("TextExtractor");
    workflowConfig.addStage(stage2Config);

    
    //    StageConfiguration stage2Config = new StageConfiguration();
    //    stage2Config.setStageClass("org.myrobotlab.document.transformer.SendToSolr");
    //    stage2Config.setStageName("SendToSolr");
    //    stage2Config.setStringParam("solrUrl", "http://phobos:8983/solr/graph");
    //    workflowConfig.addStage(stage2Config);
    pipeline.setConfig(workflowConfig);
    pipeline.initalize();

    // attach the pipeline to solr.
    // 
    pipeline.attachDocumentListener(solr.getName());
    
    // start the file connector to scan the file system.
    // RSSConnector connector = (RSSConnector) Runtime.start("rss", "RSSConnector");
    FileConnector connector = (FileConnector) Runtime.start("fileconnector", "FileConnector");
    connector.setDirectory("Z:\\Music");

    // connector to pipeline connection
    connector.attachDocumentListener(pipeline.getName());

    // start the crawl!
    connector.startCrawling();
    // TODO: make sure we flush the pending batches!
    // connector.flush();
    // poll to make sure the connector is still running./
    while (ConnectorState.RUNNING.equals(connector.getState())) {
      System.out.println(".");
      Thread.sleep(1000);
    }
    // when the connector is done, tell the pipeline to flush/
    pipeline.flush();

    // 

  }

  public void initalize() throws ClassNotFoundException {
    // init the workflow server and load the pipeline config.
    if (workflowServer == null) {
      workflowServer = WorkflowServer.getInstance(this);
    }
    workflowServer.addWorkflow(workFlowConfig);
    workflowName = workFlowConfig.getName();

    // We can't drop messages! apply back pressure if the inbox is full!
    this.inbox.setBlocking(true);

  }

  // TODO: put this on a base class or something?
  @Override
  public ProcessingStatus onDocuments(List<Document> docs) {
    ProcessingStatus totalStat = ProcessingStatus.OK;
    for (Document d : docs) {
      ProcessingStatus stat = onDocument(d);
      if (ProcessingStatus.ERROR.equals(stat)) {
        totalStat = ProcessingStatus.ERROR;
      }

    }
    return totalStat;
  }

  @Override
  public boolean onFlush() {
    // here we need to pass a flush message to the workflow server
    workflowServer.flush(workflowName);
    return true;
  }

}
