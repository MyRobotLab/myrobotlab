package org.myrobotlab.document.transformer;

import java.util.ArrayList;

public class WorkflowConfiguration extends Configuration {

	ArrayList<StageConfiguration> stages;
	
	private String name = "defaultWorkflow";
	public WorkflowConfiguration() {
		// TODO Auto-generated constructor stub
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

}
