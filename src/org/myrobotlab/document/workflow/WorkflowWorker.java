package org.myrobotlab.document.workflow;

import org.myrobotlab.document.Document;

public class WorkflowWorker extends Thread {

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
				e.printStackTrace();
			} 
		}
	}

	public boolean isProcessing() {
		return processing;
	}

}
