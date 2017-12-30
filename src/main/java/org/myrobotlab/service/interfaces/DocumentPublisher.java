package org.myrobotlab.service.interfaces;

import org.myrobotlab.document.Document;

public interface DocumentPublisher {

  public String getName();

  public Document publishDocument(Document doc);

  public void addDocumentListener(DocumentListener listener);

}
