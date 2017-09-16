package org.myrobotlab.document.transformer;

import org.myrobotlab.document.Document;

public class JSoupExtractorText extends AbstractStageTest {

  @Override
  public Document createDocument() {
    // TODO Auto-generated method stub
    Document doc = new Document("test_doc_1");

    String html = "<body><a href=\"foo.html\"/></body>";

    doc.setField("html", html);

    return doc;
  }

  @Override
  public AbstractStage createStage() {
    // TODO Auto-generated method stub
    JSoupExtractor stage = new JSoupExtractor();
    return stage;
  }

  @Override
  public void validate(Document doc) {
    // TODO Auto-generated method stub
    System.out.println(doc);
  }

}
