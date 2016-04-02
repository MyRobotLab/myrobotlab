package org.myrobotlab.document.transformer;

import java.util.HashMap;
import java.util.List;

public class StageConfiguration extends Configuration {

 	// private HashMap<String, Object> config = null;

	private String stageName = "defaultStage";
	private String stageClass = "org.myrobotlab.document.transformer.AbstractStage";
	
	public StageConfiguration(String stageName, String stageClass) {
		config = new HashMap<String, Object>();
		this.stageName = stageName;
		this.stageClass = stageClass;
	}
	
	public StageConfiguration() {
		// depricate this constructor?
		config = new HashMap<String, Object>();
	}
	
	public void setListParam(String name, List<String> values) {
		config.put(name, values);
	}

	public List<String> getListParam(String name) {
		Object val = config.get(name);
		if (val instanceof List) {
			return (List<String>)val;
		}
		// TODO: null or empty list?
		return null;
	}
	
	public void setStringParam(String name, String value) {
		config.put(name, value);
	}
	
	public Integer getIntegerParam(String name, Integer defaultValue) {
		if (config.containsKey(name)) {
			Object val = config.get(name);
			if (val instanceof Integer) {
				return (Integer)val;
			} else {
				// TOOD: this value was not a string?
				return Integer.valueOf(val.toString());
			}
		} else {
			return defaultValue;
		}
	}
	
	public Boolean getBoolParam(String name, Boolean defaultValue) {
		if (config.containsKey(name)) {
			Object val = config.get(name);
			if (val instanceof Boolean) {
				return (Boolean)val;
			} else {
				// TOOD: this value was not a string?
				return Boolean.valueOf(val.toString());
			}
		} else {
			return defaultValue;
		}
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
