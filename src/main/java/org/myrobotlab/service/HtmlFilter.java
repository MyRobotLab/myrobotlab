package org.myrobotlab.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.HtmlFilterConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.TextFilter;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

/**
 * A service that will either strip out html from input text or wrap the input
 * text in html tags.
 * 
 * @author kwatters
 *
 */
public class HtmlFilter extends Service implements TextListener, TextPublisher, TextFilter {

  private static final long serialVersionUID = 1L;

  /**
   * set of text publishers publishing text "to" us
   */
  protected Set<String> publishers = new HashSet<>();

  public HtmlFilter(String n, String id) {
    super(n, id);
  }

  // helper function to add html tags
  public String addHtml(String text) {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    return c.preHtmlTag + text + c.postHtmlTag;
  }

  public void addTextListener(TextListener service) {
    attachTextListener(service);
  }

  public String getPostHtmlTag() {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    return c.postHtmlTag;
  }

  public String getPreHtmlTag() {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    return c.preHtmlTag;
  }

  public boolean isStripHtml() {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    return c.stripHtml;
  }

  @Override
  public void onText(String text) {
    // process the text and then publish the new text.
    processText(text);
  }

  @Override
  public String processText(String text) {
    HtmlFilterConfig c = (HtmlFilterConfig) config;

    invoke("publishRawText", text);

    String processedText = text;

    if (c.stripHtml) {
      // clean text
      processedText = stripHtml(text);
    } else {
      processedText = addHtml(text);
    }

    if (c.stripUrls) {
      processedText = stripUrls(processedText);
    }

    invoke("publishText", processedText);
    return processedText;
  }

  public String publishRawText(String text) {
    return text;
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
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    c.postHtmlTag = postHtmlTag;
  }

  /**
   * The string to be prepended to the input text Defaults to &lt;pre&gt;
   * 
   * @param preHtmlTag
   *          - a string to prepend to the text.
   */
  public void setPreHtmlTag(String preHtmlTag) {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    c.preHtmlTag = preHtmlTag;
  }

  /**
   * If this is true, the input text will be striped of html. If this is false,
   * the input text will get the pre and post html tags added to it.
   * 
   * @param stripHtml
   *          - if true, all content between &lt;and &gt; will be removed.
   */
  public void setStripHtml(boolean stripHtml) {
    HtmlFilterConfig c = (HtmlFilterConfig) config;
    c.stripHtml = stripHtml;
  }

  // helper function to strip html tags.
  public static String stripHtml(String text) {
    if (StringUtils.isEmpty(text))
      return text;
    String cleanText = Jsoup.parse(text).text().trim();
    return cleanText.trim();
  }

  public String stripUrls(String text) {
    String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
    // String urlPattern =
    // "<\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>";
    Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text);
    int i = 0;
    while (m.find()) {
      text = text.replace(m.group(i), "").trim();
      i++;
    }
    return text;
    // return text.replaceAll("http.*?\\s", "");
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

    Set<String> listeners = getAttached("publishText");
    config.textListeners = listeners.toArray(new String[listeners.size()]);

    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    HtmlFilterConfig config = (HtmlFilterConfig) c;

    if (config.textListeners != null) {
      for (String serviceName : config.textListeners) {
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
