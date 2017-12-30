package org.myrobotlab.document.transformer;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.myrobotlab.document.Document;

/**
 * This stage will use a jsoup selection string on html and store the resulting
 * data int the output field
 * 
 * @author kwatters
 *
 */
public class JSoupExtractor extends AbstractStage {

  private String htmlField = "html";
  private String outputField = "links";
  private String jSoupSelector = "a[href]";

  @Override
  public void startStage(StageConfiguration config) {
    if (config != null) {
      htmlField = config.getProperty(htmlField, "html");
      outputField = config.getProperty("outputField", "links");
      jSoupSelector = config.getProperty("jSoupSelector", "a[href]");
    }
  }

  @Override
  public List<Document> processDocument(Document doc) {
    for (Object o : doc.getField(htmlField)) {
      org.jsoup.nodes.Document jSoupDoc = Jsoup.parse(o.toString());
      Elements links = jSoupDoc.select(jSoupSelector);
      for (Element link : links) {
        doc.addToField(outputField, link);
      }
    }

    return null;
  }

  @Override
  public void stopStage() {
    // TODO Auto-generated method stub

  }

  @Override
  public void flush() {
    // TODO Auto-generated method stub

  }

  public String getHtmlField() {
    return htmlField;
  }

  public void setHtmlField(String htmlField) {
    this.htmlField = htmlField;
  }

  public String getOutputField() {
    return outputField;
  }

  public void setOutputField(String outputField) {
    this.outputField = outputField;
  }

  public String getjSoupSelector() {
    return jSoupSelector;
  }

  public void setjSoupSelector(String jSoupSelector) {
    this.jSoupSelector = jSoupSelector;
  }

}
