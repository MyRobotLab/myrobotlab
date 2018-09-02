package org.myrobotlab.document.transformer;

import java.util.List;

import org.myrobotlab.document.Document;

public class CreateStaticTeaser extends AbstractStage {

  private String textField = "text";
  private String teaserField = "teaser";

  @Override
  public void startStage(StageConfiguration config) {
    // TODO: support processing a byte array on a document.
    // rather than just a reference for on disk
      textField = config.getProperty("textField", "text");
      teaserField = config.getProperty("teaserField", "teaser");
  }

  @Override
  public List<Document> processDocument(Document doc) {
    // TODO Auto-generated method stub
    // for now, we'll just grab the first sentence?  
    // TODO: better sentence detection
    // Also. only pull the first value from the text field
    
    if (!doc.hasField(textField)) {
      return null;
    }
    
    
    String text = doc.getField(textField).get(0).toString();
    
    if (text.isEmpty())
      return null;
    
    // now create the teaser
    
    String teaser = createTeaser(text);
    doc.setField(teaserField, teaser);
    
    return null;
  }

  private String createTeaser(String text) {
    // TODO : something much better than this !!
    String[] parts = text.split("\\.");
    String teaser = parts[0];
    return teaser;
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
