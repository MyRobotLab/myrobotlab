package org.myrobotlab.document.transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class MathValues extends AbstractStage {

  public final static Logger log = LoggerFactory.getLogger(MathValues.class.getCanonicalName());

  private List<String> inputFields = null;
  private String outputField = null;
  private String expressionString = null;
  // TODO: can I re-use this?
  private Expression expr;

  @Override
  public void startStage(StageConfiguration config) {

    // TODO: implement me!
    // can i reuse the expresion?
    inputFields = config.getListParam("inputFields");
    outputField = config.getProperty("outputField");
    expressionString = config.getProperty("expressionString");

    // create the expression builder
    ExpressionBuilder eBuilder = new ExpressionBuilder(expressionString);
    // set up the variables.
    eBuilder.variables(new HashSet<String>(inputFields));
    // compile the expression.
    expr = eBuilder.build();

  }

  @Override
  public List<Document> processDocument(Document doc) {
    // divide the double values in 2 fields, store the result in the quotent
    // field.
    for (String inField : inputFields) {
      if (!doc.hasField(inField)) {
        // doc missing one of the input fields?
        // TODO: maybe we want to control this behavior (ignore unset fields?)
        return null;
      }
    }

    ArrayList<Double> results = new ArrayList<Double>();
    int size = doc.getField(inputFields.get(0)).size();
    for (int i = 0; i < size; i++) {
      // load the variables into the expression
      for (String inField : inputFields) {
        Double d = convertToDouble(doc.getField(inField).get(i));
        if (d == null) {
          // we weren't able to parse one of the input variables.
          // TODO: log a warning or something?
          return null;
        }
        expr.setVariable(inField, d);
      }
      // Division by zero might result?
      try {
        Double result = expr.evaluate();
        results.add(result);
      } catch (ArithmeticException e) {
        log.info("Division by zero error! {}", doc.getId());
      }

    }
    for (Double v : results) {
      doc.addToField(outputField, v);
    }

    return null;
  }

  private Double convertToDouble(Object obj) throws ClassCastException {
    Double doubleVal = null;
    if (obj instanceof Integer) {
      doubleVal = new Double(((Integer) obj).intValue());
    } else if (obj instanceof Double) {
      doubleVal = (Double) obj;
    } else if (obj instanceof String) {
      // ... this could throw an exception,
      try {
        // TODO: some cleaning of the string to only allow numbers and periods
        // and minus signs...
        String strVal = ((String) obj).replaceAll(",", "").trim();
        doubleVal = Double.valueOf(strVal);
      } catch (NumberFormatException e) {
        // ok. we couldn't cast it.
        log.info("Unable to parse {} into a double.", obj);
        return null;
      }
    } else {
      throw new ClassCastException("Cannot convert " + obj.getClass().getName() + " to Double.");
    }

    return doubleVal;
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
