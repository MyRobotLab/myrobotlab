package org.myrobotlab.document.transformer;

import static org.junit.Assert.assertEquals;

import org.myrobotlab.document.Document;

public class JSoupExtractorTest extends AbstractStageTest {

  @Override
  public Document createDocument() {
    Document doc = new Document("test_doc_1");
    String html = "<body><a href=\"foo.html\"/></body>";
    doc.setField("html", html);
    return doc;
  }

  @Override
  public AbstractStage createStage() {
    JSoupExtractor stage = new JSoupExtractor();
    return stage;
  }

  @Override
  public void validate(Document doc) {
    // default behavior, make sure the a[href]'s have been extracted
    assertEquals("<a href=\"foo.html\"></a>", doc.getField("links").get(0).toString());
  }

}
