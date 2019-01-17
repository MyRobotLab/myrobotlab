package org.myrobotlab.document.transformer;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.myrobotlab.document.Document;

/**
 * This stage will join together a list of values into a single string value
 * with a separator.
 * 
 * @author kwatters
 *
 */
public class JoinFieldValues extends AbstractStage {

  private String inputField;
  private String outputField;
  private String joinString;

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      inputField = config.getProperty("inputField");
      outputField = config.getProperty("outputField");
      joinString = config.getProperty("joinString");
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    if (doc.hasField(inputField)) {
      String joinedValues = StringUtils.join(doc.getField(inputField), joinString);
      doc.setField(outputField, joinedValues);
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
