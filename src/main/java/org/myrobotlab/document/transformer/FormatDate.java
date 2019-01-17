package org.myrobotlab.document.transformer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This stage will take the values in the inputField and attempt to parse them
 * into a date object based on the formatString. The successfully parsed values
 * will be stored in the outputField. The values will overwrite the outputField
 * values.
 * 
 * @author kwatters
 *
 */
public class FormatDate extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(FormatDate.class.getCanonicalName());

  private String inputField = "date";
  private String outputField = "date_string";
  private String formatString = "yyymmdd";
  private SimpleDateFormat sdf = null;

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      inputField = config.getProperty("inputField");
      outputField = config.getProperty("outputField", "date");
      formatString = config.getStringParam("formatString", formatString);
    }
    // compile the date string parsers.
    sdf = new SimpleDateFormat(formatString);
  }

  @Override
  public List<Document> processDocument(Document doc) {
    if (!doc.hasField(inputField)) {
      return null;
    }
    ArrayList<String> formattedDates = new ArrayList<String>();
    for (Object val : doc.getField(inputField)) {
      if (val instanceof Date) {
        String formattedDate = sdf.format(val);
        formattedDates.add(formattedDate);
      }
    }
    // TODO: configure input/output overwrite vs append mode.
    if (inputField.equals(outputField)) {
      doc.removeField(outputField);
    }
    for (String d : formattedDates) {
      doc.addToField(outputField, d);
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
