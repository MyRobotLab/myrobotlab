package org.myrobotlab.document.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.document.transformer.StageConfiguration;
import org.myrobotlab.document.transformer.WorkflowConfiguration;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.document.transformer.AbstractStage;

public class Workflow {

	private int numWorkerThreads = 10;
	private int queueLength = 50;
	private LinkedBlockingQueue<Document> queue = new LinkedBlockingQueue<Document>(queueLength);
	private Document stopDoc;

	private String name = "defaultWorkflow";
	// A workflow is a list of stages
	private ArrayList<AbstractStage> stages;

	private WorkflowWorker[] workers;

	public void initialize() {
		// TODO: do this better?
		stopDoc = new Document(null);
		workers = new WorkflowWorker[numWorkerThreads];
		for (int i = 0; i < numWorkerThreads; i++) {
			initializeWorkerThread(i);
		}
	}

	private void initializeWorkerThread(int threadNum) {
		WorkflowWorker worker = new WorkflowWorker(this);
		worker.start();
		workers[threadNum] = worker;
	}

	public Workflow(WorkflowConfiguration workflowConfig) throws ClassNotFoundException {
		ClassLoader classLoader = Workflow.class.getClassLoader();
		// create a workflow
		stages = new ArrayList<AbstractStage>();
		for (StageConfiguration stageConf : workflowConfig.getStages()) {
			String stageClass = stageConf.getStageClass();
			System.out.println("Starting stage :"  + stageConf.getStageName() + " class=" + stageConf.getStageClass());
			Class<?> sc = Workflow.class.getClassLoader().loadClass(stageClass);
			try {
				AbstractStage stageInst = (AbstractStage) sc.newInstance();
				stageInst.startStage(stageConf);
				addStage(stageInst);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// We need to load a config
		// then we need to create each of the stages for the config
		// and add those to our stage list.
		
		this.name = workflowConfig.getName();
	}


	public void addStage(AbstractStage stage) {
		stages.add(stage);
	}

	public void processDocument(Document doc) throws InterruptedException {

		// put the document on the processing queue.
		if (doc != null) {
			queue.put(doc);
		} else {
			queue.put(doc);
		}

	}

	public Document getDocToProcess() throws InterruptedException {

		Document doc = queue.take();
		// TODO: cleaner way to do this is have a queue of a new class, which
		// has the Item in it and meta information to tell us when to stop / etc
		if (doc.getId() == stopDoc.getId()) {
			// For now, we push it back on, so that in multi-worker environment
			// all of them get the stop notification.
			queue.put(stopDoc);
			return null;
		} else {
			// System.out.println("Pulled doc to process : " + doc.getId());
			return doc;
		}

	}

	public void processDocumentInternal(Document doc, int stageOffset) {
		// TODO:
		int i = 0;
		for (AbstractStage s : stages.subList(i, stages.size())) {
			List<Document> childDocs = s.processDocument(doc);
			i++;
			if (childDocs != null) {
				// process each of the children docs down the rest of the pipeline
				for (Document childDoc : childDocs) {
					processDocumentInternal(childDoc, i);
				}
			}
			// TODO:should I create a completely new concept for
			// callbacks?
			if (doc.getStatus().equals(ProcessingStatus.DROP)) {
				// if it's a drop, break here.
				break;
			}
		}
	}
	
	public void flush() {
		// TODO Auto-generated method stub
		// TODO: Make this wait for a particular message sequence id to finish.
		// TODO: Or make it block here.
		while (!queue.isEmpty()) {
			try {
				// TODO: give a logger object to this class.
				// TODO: review this for threading issues and concurrency
				System.out.println("Waiting for workflow flush.");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} 

		// now wait for the threads to no longer be running
		while (true) {
			boolean oneIsRunning = false;
			for (int i = 0; i < numWorkerThreads; i++) {
				oneIsRunning |= workers[i].isProcessing();
			}
			if (!oneIsRunning) {
				break;
			}
			try {
				System.out.println("Workers are still running...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (AbstractStage s : stages) {
			s.flush();
		}
		
		System.out.println("Workflow flushed.");


	}

	public String getName() {
		return name;
	}

}
