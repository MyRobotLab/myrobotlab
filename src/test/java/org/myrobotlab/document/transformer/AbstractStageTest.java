package org.myrobotlab.document.transformer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.document.Document;
import org.myrobotlab.test.AbstractTest;

@Ignore
public abstract class AbstractStageTest extends AbstractTest {

  public abstract Document createDocument();

  public abstract AbstractStage createStage();

  @Before
  public void init() {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void test() {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    AbstractStage stage = createStage();
    Document doc = createDocument();
    stage.processDocument(doc);
    validate(doc);
  }

  public abstract void validate(Document doc);

}
