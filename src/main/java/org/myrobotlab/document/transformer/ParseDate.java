package org.myrobotlab.document.transformer;

import java.text.ParseException;
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
public class ParseDate extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(ParseDate.class.getCanonicalName());

  private String inputField = null;
  private String outputField = "date";
  private List<String> formatStrings;
  private List<SimpleDateFormat> sdfs = null;

  @Override
  public void startStage(StageConfiguration config) {
    // TODO Auto-generated method stub
    if (config != null) {
      inputField = config.getProperty("inputField");
      outputField = config.getProperty("outputField", "date");
      formatStrings = config.getListParam("formatStrings");
    }
    // compile the date string parsers.
    sdfs = new ArrayList<SimpleDateFormat>();
    for (String formatString : formatStrings) {
      SimpleDateFormat sdf = new SimpleDateFormat(formatString);
      sdfs.add(sdf);
    }

  }

  @Override
  public List<Document> processDocument(Document doc) {

    if (!doc.hasField(inputField)) {
      return null;
    }
    ArrayList<Date> dates = new ArrayList<Date>();
    for (Object val : doc.getField(inputField)) {
      if (val instanceof String) {
        boolean parsed = false;
        for (SimpleDateFormat sdf : sdfs) {
          try {
            Date d = sdf.parse(val.toString());
            dates.add(d);
            parsed = true;
            // we found a match
            break;
          } catch (ParseException e) {
            // log.warn("Unparsable date string doc id: {} value: {}",
            // doc.getId(), val);
            // e.printStackTrace();
          }
        }
        if (!parsed) {
          log.warn("Doc ID : {} Did not parse date string: {}", doc.getId(), val);
        }

      }
    }
    // TODO: configure input/output overwrite vs append mode.
    if (inputField.equals(outputField)) {
      doc.removeField(outputField);
    }
    for (Date d : dates) {
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
