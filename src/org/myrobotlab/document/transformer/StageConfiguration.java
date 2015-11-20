package org.myrobotlab.document.transformer;

import java.util.HashMap;

public class StageConfiguration extends Configuration {

 	// private HashMap<String, Object> config = null;

	private String stageName = "defaultStage";
	private String stageClass = "com.kmwllc.brigade.stage.AbstractStage";
	
	public StageConfiguration() {
		config = new HashMap<String, Object>();
	}
	
	public void setStringParam(String name, String value) {
		config.put(name, value);
	}
	
	public String getStringParam(String name) {
		if (config.containsKey(name)) {
			Object val = config.get(name);
			if (val instanceof String) {
				return (String)val;
			} else {
				// TOOD: this value was not a string?
				return val.toString();
			}
		} else {
			return null;
		}
	}

	public String getStageName() {
		return stageName;
	}

	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	public String getStageClass() {
		return stageClass;
	}

	public void setStageClass(String stageClass) {
		this.stageClass = stageClass;
	}

}
