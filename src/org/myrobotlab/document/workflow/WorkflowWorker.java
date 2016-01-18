package org.myrobotlab.document.workflow;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class WorkflowWorker extends Thread {
	public final static Logger log = LoggerFactory.getLogger(WorkflowWorker.class);
	private Workflow w;
	boolean processing = false;
	
	WorkflowWorker(Workflow workflow) {
		this.w = workflow;
	}

	public void run() {
		Document doc;
		boolean running = true;
		while (running) {
			try {
				doc = w.getDocToProcess();
				if (doc == null) {
					running = false;
				} else {
					processing = true;
					// process from the start of the workflow
					w.processDocumentInternal(doc, 0);
					processing = false;
				}
			} catch (InterruptedException e) {
				// TODO: handle these properly
				log.warn("Workflow Worker Died! {}", e.getMessage());
				e.printStackTrace();
				
			} 
		}
	}

	public boolean isProcessing() {
		return processing;
	}

}
