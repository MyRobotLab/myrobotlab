package org.myrobotlab.document.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.myrobotlab.document.Document;

/**
 * This stage will use a regex to find a pattern in a string field and store the
 * matched text into the output field.
 *
 * The list of keepGroups tells the RegexEtractor which groups from the regular
 * expression to keep. Groups are concatenated to form the output value.
 *
 * @author kwatters, dmeehl
 *
 */
public class RegexExtractor extends AbstractStage {

  private String inputField = null;
  private String outputField = null;
  private List<Integer> keepGroups = null;
  private String regex = null;

  private Pattern pattern;

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      inputField = config.getProperty("inputField", "text");
      outputField = config.getProperty("outputField", "entity");
      List<String> keepGroupsStr = config.getListParam("keepGroups");
      regex = config.getProperty("regex");
      processOnlyNull = config.getBoolParam("processOnlyNull", processOnlyNull);

      keepGroups = new ArrayList<Integer>();
      if (keepGroupsStr == null) {
        keepGroups.add(1);
      } else {
        for (String groupNum : keepGroupsStr) {
          keepGroups.add(Integer.parseInt(groupNum));
        }
      }
    }
    pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  }

  @Override
  public List<Document> processDocument(Document doc) {
    if (!doc.hasField(inputField)) {
      return null;
    }

    if (processOnlyNull && doc.hasField(outputField)) {
      return null;
    }

    List<String> matches = new ArrayList<String>();
    for (Object o : doc.getField(inputField)) {
      String text = o.toString();
      Matcher matcher = pattern.matcher(text);
      if (matcher.matches() && matcher.groupCount() > 0) {
        String match = "";
        for (Integer num : keepGroups) {
          match += matcher.group(num);
        }
        matches.add(match);
      }
    }

    doc.removeField(outputField);
    for (String match : matches) {
      doc.addToField(outputField, match);
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

  public String getInputField() {
    return inputField;
  }

  public void setInputField(String inputField) {
    this.inputField = inputField;
  }

  public String getOutputField() {
    return outputField;
  }

  public void setOutputField(String outputField) {
    this.outputField = outputField;
  }

  public String getRegex() {
    return regex;
  }

  public void setRegex(String regex) {
    this.regex = regex;
  }

}
