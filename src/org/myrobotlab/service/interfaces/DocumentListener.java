package org.myrobotlab.service.interfaces;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;

public interface DocumentListener {

	public String getName();

	public ProcessingStatus onDocument(Document doc);
}
