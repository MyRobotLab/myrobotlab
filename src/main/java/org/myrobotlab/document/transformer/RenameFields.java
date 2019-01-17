package org.myrobotlab.document.transformer;

import java.util.List;
import java.util.Map;

import org.myrobotlab.document.Document;

/**
 * This stage will rename the field on a document.
 * 
 * @author kwatters
 *
 */
public class RenameFields extends AbstractStage {

  private Map<String, String> fieldNameMap;

  @Override
  public void startStage(StageConfiguration config) {
    fieldNameMap = config.getMapProperty("fieldNameMap");
  }

  @Override
  public List<Document> processDocument(Document doc) {
    for (String oldName : fieldNameMap.keySet()) {
      if (!doc.hasField(oldName)) {
        return null;
      }
      String newName = fieldNameMap.get(oldName);
      if (!newName.equals(oldName)) {
        for (Object o : doc.getField(oldName)) {
          doc.addToField(newName, o);
        }
        doc.removeField(oldName);
      }
    }
    return null;
  }

  @Override
  public void stopStage() {
    // NO-OP
  }

  @Override
  public void flush() {
    // NO-OP
  }

}
