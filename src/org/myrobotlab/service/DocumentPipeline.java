package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.service.interfaces.DocumentListener;
import org.myrobotlab.service.interfaces.DocumentPublisher;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.document.workflow.WorkflowMessage;
import org.myrobotlab.document.workflow.WorkflowServer;
import org.myrobotlab.framework.Service;

public class DocumentPipeline extends Service implements DocumentListener,DocumentPublisher {

	private static final long serialVersionUID = 1L;
	private String rssUrl = "http://www.myrobotlab.org/rss.xml";
	
	private WorkflowConfiguration config;
	private WorkflowServer workflowServer;
	private String workflowName = "default";
	
	public DocumentPipeline(String reservedKey) {
		super(reservedKey);
	}
	
	@Override
	public String[] getCategories() {
		// TODO Auto-generated method stub
		return new String[]{"data"};
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "This service will pass a document through a document processing pipeline made up of transformers.";
	}

	public String getRssUrl() {
		return rssUrl;
	}

	public void setUrl(String rssUrl) {
		this.rssUrl = rssUrl;
	}

	private void setConfig(WorkflowConfiguration workflowConfig) {
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
		
	}

	@Override
	public ProcessingStatus onDocument(Document doc) {
		// TODO Auto-generated method stub
		// process the document!  return a processing status!
		
		WorkflowMessage msg = new WorkflowMessage();
		msg.setDoc(doc);
		// for now only default workflow supported (1 workflow per DocumentPipeline service i guess?)
		msg.setWorkflow(workflowName);
		// TODO: the type message should be add/update/delete sort of message types.
		//msg.setType(type);
		try {
			workflowServer.processMessage(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO: this isn't correct!
			return ProcessingStatus.ERROR;
		}
		
		// TODO: we need to properly track the status of the message we just sent off..
		// callbacks should be re-designed here .. this processing status is kinda not correct.
		return ProcessingStatus.OK;
	}
	
	public void flush() {
		workflowServer.flush(workflowName);
	}
	
	public static void main(String[] args) throws Exception {

		// create the pipeline service in MRL
		DocumentPipeline pipeline = (DocumentPipeline)Runtime.start("docproc", "DocumentPipeline");
		
		// pipeline.workflowName = "default";
		// create a workflow to load into that pipeline service
		WorkflowConfiguration workflowConfig = new WorkflowConfiguration();
		workflowConfig.setName("default");
		StageConfiguration stage1Config = new StageConfiguration();
		stage1Config.setStageClass("org.myrobotlab.document.transformer.SetStaticFieldValue");
		stage1Config.setStageName("SetTableField");
		stage1Config.setStringParam("table", "MRL");
		workflowConfig.addStage(stage1Config);

		StageConfiguration stage2Config = new StageConfiguration();
		stage2Config.setStageClass("org.myrobotlab.document.transformer.SendToSolr");
		stage2Config.setStageName("SendToSolr");
		stage2Config.setStringParam("solrUrl", "http://phobos:8983/solr/collection1");
		workflowConfig.addStage(stage2Config);
		
		pipeline.setConfig(workflowConfig);
		pipeline.initalize();
		
		//Solr solr = (Solr)Runtime.start("solr", "Solr");
		//solr.setSolrUrl("http://phobos:8983/solr/sagewell");
		//
		RSSConnector connector = (RSSConnector)Runtime.start("rss", "RSSConnector");
		connector.addDocumentListener(pipeline);
		connector.startCrawling();
		
		// TODO: make sure we flush the pending batches!  
		//connector.flush();
		// poll to make sure the connector is still running./
		while (ConnectorState.RUNNING.equals(connector.getState())) {
			System.out.println(".");
			Thread.sleep(1000);			
		}
		// when the connector is done, tell the pipeline to flush/
		pipeline.flush();
		
		//wee! news!
		
	}

	private void initalize() throws ClassNotFoundException {
		// init the workflow server and load the pipeline config.
		if (workflowServer == null) {
			workflowServer = WorkflowServer.getInstance();
		}
		workflowServer.addWorkflow(config);
		this.workflowName = config.getName();
		
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
	
}
