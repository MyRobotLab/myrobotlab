package org.myrobotlab.document.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

public class DictionaryLookup extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(DictionaryLookup.class.getCanonicalName());

  private String inputField;
  private List<String> outputFields;
  private String dictionaryFile;
  private HashMap<String, List<String>> dictionary;
  private String defaultValue = "Unknown";

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      inputField = config.getProperty("inputField", "text");
      outputFields = config.getListParam("outputFields");
      dictionaryFile = config.getProperty("dictionaryFile", "mydict.csv");

      if (outputFields == null) {
        String outputField = config.getProperty("outputField");
        if (!StringUtil.isEmpty(outputField)) {
          outputFields = new ArrayList<String>();
          outputFields.add(outputField);
        }
      }
    }

    try {
      dictionary = DictionaryLoader.getInstance().loadDictionary(dictionaryFile);
    } catch (IOException e) {
      log.warn("Error loading dictionary {} IOException {}", dictionaryFile, e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    /*
     * input field values: I1, X1, I2, Y1, I3 output fields: out1, out2, out3
     * dict: I1, A1, B1, C1 I2, A2, B2, C2 I3, A3, B3, C3
     * 
     * Result: out1: A1, A2, A3 out2: B1, B2, B3 out3: C1, C2, C3
     */

    if (!doc.hasField(inputField)) {
      return null;
    }

    ArrayList<List<String>> lookedupValues = new ArrayList<List<String>>();
    for (Object o : doc.getField(inputField)) {
      if (o == null) {
        continue;
      }
      List<String> dictCols = dictionary.get(o.toString());
      if (dictCols != null) {
        lookedupValues.add(dictCols);
      }
    }

    for (int i = 0; i < outputFields.size(); i++) {
      String outputField = outputFields.get(i);
      if (inputField.equals(outputField)) {
        doc.removeField(outputField);
      }
      for (List<String> dictCols : lookedupValues) {
        String val = dictCols.get(i);
        if (!StringUtil.isEmpty(val)) {
          doc.addToField(outputField, val);
        } else {
          // handle an empty value?
          if (defaultValue != null) {
            doc.addToField(outputField, defaultValue);
          }
        }
      }
    }

    // this stage doesn't emit child docs.
    return null;
  }

  @Override
  public void stopStage() {

  }

  @Override
  public void flush() {

  }

}
