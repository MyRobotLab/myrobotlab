package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;

/**
 * DropDocument - if the document contains a particular field value, drop this
 * document from the workflow. input field input value
 * 
 * @author kwatters
 *
 */
public class DropDocument extends AbstractStage {

  private String field;
  private String value;

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      field = config.getProperty("field", null);
      value = config.getProperty("value", null);
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    // TODO Auto-generated method stub
    if (doc.hasField(field)) {
      for (Object o : doc.getField(field)) {
        if (o.equals(value)) {
          doc.setStatus(ProcessingStatus.DROP);
          break;
        }
      }
    }
    return null;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush() {
    // TODO Auto-generated method stub

  }

}
