package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;

public interface DocumentListener {

  public String getName();

  public ProcessingStatus onDocument(Document doc);

  public ProcessingStatus onDocuments(List<Document> docs);

  public boolean onFlush();

}
