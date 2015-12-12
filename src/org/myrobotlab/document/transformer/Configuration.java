package org.myrobotlab.document.transformer;

import java.util.HashMap;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class Configuration {

	// The config consists of many parts.
	// server level config
	// stage config
	// workflow /pipeline config
	// connector config
	
	protected HashMap<String, Object> config = null;
	//private XStream xstream = null;
	
	public Configuration() {
		config = new HashMap<String, Object>();
		// figure that we need to be able to serialize / deserialize
		// TODO: consider a faster driver / serializer
		// xstream = new XStream(new StaxDriver());	
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
	
	public String getProperty(String name) {
		return getStringParam(name);
	}

	public String getProperty(String name, String defaultValue) {
		String val = getStringParam(name);
		if (val == null) {
			return defaultValue;
		} else {
			return val;
		}
	}

}
