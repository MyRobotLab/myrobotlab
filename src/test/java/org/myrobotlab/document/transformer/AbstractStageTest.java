package org.myrobotlab.document.transformer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.document.Document;
import org.myrobotlab.logging.LoggingFactory;

@Ignore
public abstract class AbstractStageTest {

  @Before
  public void init() {
    LoggingFactory.init("WARN");
  }
  
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
