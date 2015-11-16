package org.myrobotlab.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The basic class that represents a document flowing through
 * the myrobotlab.  
 * 
 * Basic idea is that a document had a unique id
 * and a map of key to list of object pairs.
 * 
 * @author kwatters
 *
 */
public class Document {

	private String id;
	private HashMap<String, ArrayList<Object>> data;
	private ProcessingStatus status;
	
	public Document(String id) {
		this.id = id;
		data = new HashMap<String, ArrayList<Object>>();
		status = ProcessingStatus.OK;
	}

	public ArrayList<Object> getField(String fieldName) {
		if (data.containsKey(fieldName)) {
			return data.get(fieldName);
		} else {
			return null;
		}
	}
	
	public void setField(String fieldName, ArrayList<Object> value) {
		data.put(fieldName, value);
	}
	
	public void setField(String fieldName, Object value) {
		// TODO Auto-generated method stub
		if (data.containsKey(fieldName)) {
			data.get(fieldName).add(value);
		} else {
			ArrayList<Object> values = new ArrayList<Object>();
			values.add(value);
			data.put(fieldName, values);
		}
		
	}


	public void addToField(String fieldName, Object value) {
		if (data.containsKey(fieldName) && (data.get(fieldName) != null)) {
			data.get(fieldName).add(value);
		} else {
			ArrayList<Object> values = new ArrayList<Object>();
			values.add(value);
			data.put(fieldName, values);
		}
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean hasField(String fieldName) {
		return data.containsKey(fieldName);
	}

	/**
	 * Return a set of all fields on a given document.
	 * This is unordered.
	 * 
	 * @return
	 */
	public Set<String> getFields() {
		// TODO Auto-generated method stub
		return data.keySet();
//		return null;
	}

	public void removeField(String oldName) {
		// TODO Auto-generated method stub
		data.remove(oldName);
		
	}

	public ProcessingStatus getStatus() {
		return status;
	}

	public void setStatus(ProcessingStatus status) {
		this.status = status;
	}

	
	
}
