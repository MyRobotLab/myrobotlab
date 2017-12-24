package org.myrobotlab.document.transformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.myrobotlab.document.Document;

public class MathValuesTest extends AbstractStageTest {

  @Override
  public Document createDocument() {
    // TODO Auto-generated method stub
    Document d = new Document("doc_1");
    d.setField("title", "MRL Cookbook.");
    d.setField("x", 2);
    d.setField("y", 3);
    return d;
  }

  @Override
  public AbstractStage createStage() {
    MathValues stage = new MathValues();
    StageConfiguration config = new StageConfiguration("mathexpr", "org.myrobotlab.document.transformer.MathValues");
    List<String> vars = new ArrayList<String>();
    vars.add("x");
    vars.add("y");
    config.setListParam("inputFields", vars);
    config.setStringParam("outputField", "z");
    config.setStringParam("expressionString", "x + y");
    stage.startStage(config);
    return stage;
  }

  @Override
  public void validate(Document doc) {
    Assert.assertEquals(doc.getField("z").get(0), 5.0);
  }

}
