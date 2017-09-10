package org.myrobotlab.document.transformer;

import org.junit.Assert;
import org.myrobotlab.document.Document;

public class SetStaticFieldValueTest extends AbstractStageTest {

  @Override
  public Document createDocument() {
    // TODO Auto-generated method stub
    Document d = new Document("doc_1");
    d.setField("title", "MRL Cookbook.");
    return d;
  }

  @Override
  public AbstractStage createStage() {
    // TODO Auto-generated method stub
    SetStaticFieldValue stage = new SetStaticFieldValue();
    stage.setFieldName("text");
    stage.setValue("This is some text.");
    return stage;
  }

  @Override
  public void validate(Document doc) {
    // TODO Auto-generated method stub
    Assert.assertEquals(doc.getField("text").get(0), "This is some text.");
  }

}
