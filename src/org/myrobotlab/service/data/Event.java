package org.myrobotlab.service.data;

import java.io.Serializable;

public class Event implements Serializable {
	private static final long serialVersionUID = 1L;
	String source;
	
	public String getSource(){
		return source;
	}
	
}
