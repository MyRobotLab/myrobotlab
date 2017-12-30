package org.myrobotlab.document.transformer;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This stage will iterate the values of the inputField and attempt to cast them
 * to a double. The values will be stored in the outputField. Values in the
 * output field will be overwritten.
 * 
 * @author kwatters
 *
 */
public class CastValuesToDouble extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(CastValuesToDouble.class);
  private String inputField = null;
  private String outputField = null;

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      inputField = config.getProperty("inputField");
      outputField = config.getProperty("outputField");
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    // throw away malformed values.
    if (!doc.hasField(inputField)) {
      return null;
    }
    ArrayList<Double> doubles = new ArrayList<Double>();
    for (Object val : doc.getField(inputField)) {

      try {
        double i = Double.valueOf(val.toString().replaceAll(",", ""));
        doubles.add(i);
      } catch (NumberFormatException e) {
        log.warn("Failed to cast value to double: doc id: {} field: {} value: {}", doc.getId(), inputField, val);
        // e.printStackTrace();
        // ??
        // doc.setStatus(ProcessingStatus.ERROR);
      }
    }

    doc.removeField(outputField);
    for (Double i : doubles) {
      doc.addToField(outputField, i);
    }

    return null;
  }

  @Override
  public void stopStage() {
    // NOOP

  }

  @Override
  public void flush() {
    // NOOP
  }

}
