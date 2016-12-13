package org.myrobotlab.document.transformer;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.document.Document;

@Ignore
public abstract class AbstractStageTest {

  public abstract Document createDocument();

  public abstract AbstractStage createStage();

  public abstract void validate(Document doc);

  @Test
  public void test() {
    AbstractStage stage = createStage();
    Document doc = createDocument();
    stage.processDocument(doc);
    validate(doc);
  }

}
