package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

/**
 * A service that will either strip out html from input text or wrap the input
 * text in html tags.
 * 
 * @author kwatters
 *
 */
public class HtmlFilter extends Service implements TextListener, TextPublisher {

  private static final long serialVersionUID = 1L;

  // true will strip html, false will add html
  private boolean stripHtml = true;
  // if stripHtml is false these tags are used to wrap the input text
  private String preHtmlTag = "<pre>";
  private String postHtmlTag = "</pre>";

  public static void main(String[] args) {
    LoggingFactory.init("INFO");

    try {
      Runtime.createAndStart("gui", "SwingGui");
      Runtime.createAndStart("python", "Python");
      HtmlFilter htmlFilter = (HtmlFilter) Runtime.createAndStart("htmlFilter", "HtmlFilter");
      log.info(">>>>>>>>>>" + htmlFilter.stripHtml("This is <a>foo</a> bar."));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public HtmlFilter(String reservedKey) {
    super(reservedKey);
  }

  // helper function to add html tags
  public String addHtml(String text) {
    return preHtmlTag + text + postHtmlTag;
  }

  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  public String getPostHtmlTag() {
    return postHtmlTag;
  }

  public String getPreHtmlTag() {
    return preHtmlTag;
  }

  public boolean isStripHtml() {
    return stripHtml;
  }

  @Override
  public void onText(String text) {
    // process the text and then publish the new text.
    if (stripHtml) {
      String cleanText = stripHtml(text);
      invoke("publishText", cleanText);
    } else {
      String htmlText = addHtml(text);
      invoke("publishText", htmlText);
    }
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  /**
   * The string to be appended to the input text Defaults to &lt;/pre&gt;
   * 
   * @param postHtmlTag - a string to append to the text
   */
  public void setPostHtmlTag(String postHtmlTag) {
    this.postHtmlTag = postHtmlTag;
  }

  /**
   * The string to be prepended to the input text Defaults to &lt;pre&gt;
   * 
   * @param preHtmlTag - a string to prepend to the text.
   */
  public void setPreHtmlTag(String preHtmlTag) {
    this.preHtmlTag = preHtmlTag;
  }

  /**
   * If this is true, the input text will be striped of html. If this is false,
   * the input text will get the pre and post html tags added to it.
   * 
   * @param stripHtml - if true, all content between &lt;and &gt; will be removed.
   */
  public void setStripHtml(boolean stripHtml) {
    this.stripHtml = stripHtml;
  }

  // helper function to strip html tags.
  public String stripHtml(String text) {
    // TODO: something fancier but this works for now.
    String cleanText = text.replaceAll("\\<.*?\\>", " ");
    cleanText = cleanText.replaceAll("  ", " ");
    return cleanText.trim();
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(HtmlFilter.class.getCanonicalName());
    meta.addDescription("This service will strip html markup from the input text");
    meta.addCategory("data", "filter");

    return meta;
  }

}
