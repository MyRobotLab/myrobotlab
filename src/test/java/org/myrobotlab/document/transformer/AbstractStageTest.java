package org.myrobotlab.document.transformer;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.document.Document;

@Ignore
public abstract class AbstractStageTest {

  public abstract Document createDocument();

  public abstract AbstractStage createStage();

  public abstract void validate(Document doc);

  public void validateChildren(List<Document> docs) {
    // NoOp most stages don't return children docs.
  };

  @Test
  public void test() {
    AbstractStage stage = createStage();
    Document doc = createDocument();
    List<Document> children = stage.processDocument(doc);
    validate(doc);
    validateChildren(children);
  }

}
