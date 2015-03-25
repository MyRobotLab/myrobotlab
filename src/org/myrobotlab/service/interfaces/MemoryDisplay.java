package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;
import org.myrobotlab.memory.Node;

public interface MemoryDisplay {

	public void clear();

	public void display(Node node);

	/*
	 * public void displayFrame(SerializableImage img); public void
	 * displayAttributes(HashMap<String, Object> data); public void
	 * displayStatus(String status); public void display(OpenCVData data);
	 */
	public void displayStatus(Status status);

}
