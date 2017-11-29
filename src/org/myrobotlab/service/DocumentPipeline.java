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
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;

public class DocumentPipeline extends Service implements DocumentListener, DocumentPublisher {

  private static final long serialVersionUID = 1L;

  private WorkflowConfiguration config;
  private transient WorkflowServer workflowServer;
  private String workflowName = "default";

  public DocumentPipeline(String reservedKey) {
    super(reservedKey);
  }

  static public String[] getCategories() {
    // TODO Auto-generated method stub
    return new String[] { "data" };
  }

  public void setConfig(WorkflowConfiguration workflowConfig) {
    this.config = workflowConfig;
  }

  @Override
  public Document publishDocument(Document doc) {
    // publish the document to the framework
    return doc;
  }

  @Override
  public void addDocumentListener(DocumentListener listener) {
    // TODO Auto-generated method stub
    // ??
    // subscribe("publishDocument", topicMethod, callbackName, callbackMethod);

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

    workflowServer.flush(workflowName);
    // TODO: what if our inbox isn't empty?

  }

  public static void main(String[] args) throws Exception {

    // create the pipeline service in MRL
    DocumentPipeline pipeline = (DocumentPipeline) Runtime.start("docproc", "DocumentPipeline");

    // pipeline.workflowName = "default";
    // create a workflow to load into that pipeline service
    WorkflowConfiguration workflowConfig = new WorkflowConfiguration("default");
    workflowConfig.setName("default");
    StageConfiguration stage1Config = new StageConfiguration();
    stage1Config.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
    stage1Config.setStageName("SetTableField");
    stage1Config.setStringParam("table", "MRL");
    workflowConfig.addStage(stage1Config);

    StageConfiguration stage2Config = new StageConfiguration();
    stage2Config.setStageClass("org.myrobotlab.document.transformer.SendToSolr");
    stage2Config.setStageName("SendToSolr");
    stage2Config.setStringParam("solrUrl", "http://phobos:8983/solr/graph");
    workflowConfig.addStage(stage2Config);

    pipeline.setConfig(workflowConfig);
    pipeline.initalize();

    RSSConnector connector = (RSSConnector) Runtime.start("rss", "RSSConnector");
    connector.addDocumentListener(pipeline);
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

    // wee! news!

  }

  public void initalize() throws ClassNotFoundException {
    // init the workflow server and load the pipeline config.
    if (workflowServer == null) {
      workflowServer = WorkflowServer.getInstance();
    }
    workflowServer.addWorkflow(config);
    workflowName = config.getName();

    // We can't drop messages! apply back pressure if the inbox is full!
    this.inbox.setBlocking(true);

  }

  // TODO: put this on a base class or something?
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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(DocumentPipeline.class.getCanonicalName());
    meta.addDescription("This service will pass a document through a document processing pipeline made up of transformers");
    meta.addCategory("ingest");
    // FIXME - add service page, python script, give example of how to use
    meta.setAvailable(false);
    return meta;
  }

}
