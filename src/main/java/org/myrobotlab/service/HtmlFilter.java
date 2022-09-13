package org.myrobotlab.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.HtmlFilterConfig;
import org.myrobotlab.service.config.ServiceConfig;
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

  /**
   * set of text publishers publishing text "to" us
   */
  protected Set<String> publishers = new HashSet<>();
  
  // true will strip html, false will add html
  private boolean stripHtml = true;
  // if stripHtml is false these tags are used to wrap the input text
  private String preHtmlTag = "<pre>";
  private String postHtmlTag = "</pre>";

  public HtmlFilter(String n, String id) {
    super(n, id);
  }

  // helper function to add html tags
  public String addHtml(String text) {
    return preHtmlTag + text + postHtmlTag;
  }

  public void addTextListener(TextListener service) {
    attachTextListener(service);
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
   * @param postHtmlTag
   *          - a string to append to the text
   */
  public void setPostHtmlTag(String postHtmlTag) {
    this.postHtmlTag = postHtmlTag;
  }

  /**
   * The string to be prepended to the input text Defaults to &lt;pre&gt;
   * 
   * @param preHtmlTag
   *          - a string to prepend to the text.
   */
  public void setPreHtmlTag(String preHtmlTag) {
    this.preHtmlTag = preHtmlTag;
  }

  /**
   * If this is true, the input text will be striped of html. If this is false,
   * the input text will get the pre and post html tags added to it.
   * 
   * @param stripHtml
   *          - if true, all content between &lt;and &gt; will be removed.
   */
  public void setStripHtml(boolean stripHtml) {
    this.stripHtml = stripHtml;
  }

  // helper function to strip html tags.
  public static String stripHtml(String text) {
    if (StringUtils.isEmpty(text))
      return text;
    String cleanText = Jsoup.parse(text).text().trim();
    return cleanText.trim();
  }

  @Override
  public void attachTextListener(TextListener service) {
    if (service == null) {
      log.warn("{}.attachTextListener(null)", getName());
      return;
    }
    attachTextListener(service.getName());
  }
  
  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.error("{}.attachTextPublisher(null)", getName());
      return;
    }
    attachTextPublisher(service.getName());
  }
  
  public void attachTextPublisher(String serviceName) {
    if (serviceName == null) {
      log.error("{}.attachTextPublisher(null)", getName());
      return;
    }
    subscribe(serviceName, "publishText");
    publishers.add(serviceName);
  }

  public void detachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    unsubscribe(service.getName(), "publishText");
    publishers.remove(service.getName());
  }

  
  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  @Override
  public ServiceConfig getConfig() {
    HtmlFilterConfig config = new HtmlFilterConfig();
    
    config.stripHtml = stripHtml;
    config.preHtmlTag = preHtmlTag;
    config.postHtmlTag = postHtmlTag;
    
    Set<String> listeners = getAttached("publishText");
    config.textListeners = listeners.toArray(new String[listeners.size()]);
    
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    HtmlFilterConfig config = (HtmlFilterConfig) c;
    
    stripHtml = config.stripHtml;
    preHtmlTag = config.preHtmlTag;
    postHtmlTag = config.postHtmlTag;
    
    if (config.textListeners != null) {
      for (String serviceName: config.textListeners) {
        attachTextListener(serviceName);
      }
    }
   
    return c;
  }

  public static void main(String[] args) {
    LoggingFactory.init("INFO");
    HtmlFilter htmlFilter = (HtmlFilter) Runtime.createAndStart("htmlFilter", "HtmlFilter");
    log.info(">>>>>>>>>>" + htmlFilter.stripHtml("This is <a>foo</a> bar."));
  }
}
