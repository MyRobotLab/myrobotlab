package org.myrobotlab.document.transformer;

import org.junit.Assert;
import org.junit.Ignore;
import org.myrobotlab.document.Document;

// need to make sure the language model files are available.

@Ignore
public class OpenNLPTest extends AbstractStageTest {

  @Override
  public Document createDocument() {
    //
    Document testDoc = new Document("doc_1");
    testDoc.setField("title", "This is my document title.");
    testDoc.setField("text",
        "This is my text field.  It mentions stuff about Greg Perry and Kevin Watters.  The FAA can regulate the skies, but they don't own the airspace inside of buildings.");

    return testDoc;
  }

  @Override
  public AbstractStage createStage() {
    // TODO Auto-generated method stub
    String stageClass = "org.myrobotlab.document.transformer.OpenNLP";
    String stageName = "opennlp";
    StageConfiguration config = new StageConfiguration();
    config.setStageClass(stageClass);
    config.setStageName(stageName);
    // TODO: i don't like how I implemented the config objects...
    // TODO: make this return a config , not the stage itself?
    OpenNLP transformer = new OpenNLP();
    transformer.startStage(config);
    return transformer;
  }

  @Override
  public void validate(Document doc) {
    Assert.assertEquals("Greg Perry", doc.getField("people").get(0).toString());
    Assert.assertEquals("Kevin Watters", doc.getField("people").get(1).toString());
    System.out.println(doc);
  }

}
