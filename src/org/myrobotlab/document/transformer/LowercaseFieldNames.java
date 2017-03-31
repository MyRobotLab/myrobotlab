package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;

/**
 * This will set a field on a document with a value
 * 
 * @author kwatters
 *
 */
public class LowercaseFieldNames extends AbstractStage {

  @Override
  public void startStage(StageConfiguration config) {

  }

  @Override
  public List<Document> processDocument(Document doc) {
    String[] fields = doc.getFields().toArray(new String[doc.getFields().size()]);
    for (String fieldName : fields) {
      doc.renameField(fieldName, fieldName.toLowerCase());
    }
    return null;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub
  }

  @Override
  public void flush() {
    // Only required if this stage does any batching. NO-OP here.
  }

}
