package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;

public abstract class AbstractStage {
  // Process only when output field doesn't exist in the document
  // Stages that support this should check and handle it in their
  // processDocument()
  protected boolean processOnlyNull = false;

  public abstract void startStage(StageConfiguration config);

  public abstract List<Document> processDocument(Document doc);

  public abstract void stopStage();

  public abstract void flush();
}
