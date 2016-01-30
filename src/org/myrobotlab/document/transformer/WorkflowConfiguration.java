package org.myrobotlab.document.transformer;

import java.util.ArrayList;

public class WorkflowConfiguration extends Configuration {

	ArrayList<StageConfiguration> stages;
	
	private String name = "defaultWorkflow";
	
	private int numWorkerThreads = 1;
	private int queueLength = 50;
	
	public WorkflowConfiguration() {
		stages = new ArrayList<StageConfiguration>();
		// default workflow static config
	}
	
	public void addStage(StageConfiguration config) {
		stages.add(config);
	}

	public ArrayList<StageConfiguration> getStages() {
		// TODO Auto-generated method stub
		return stages;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNumWorkerThreads() {
		return numWorkerThreads;
	}

	public void setNumWorkerThreads(int numWorkerThreads) {
		this.numWorkerThreads = numWorkerThreads;
	}

	public int getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}

}
