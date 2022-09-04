package org.myrobotlab.service.config;

/**
 * Configuration file for HtmlFilter service
 * 
 * if stripHtml is true, html is removed.
 * if stripHtml if false, the text is wrapped with 
 * preHtmlTag and the postHtmlTag
 * 
 */
public class HtmlFilterConfig extends ServiceConfig {

  //   
  public boolean stripHtml = true;
  // 
  public String preHtmlTag = "<pre>";
  public String postHtmlTag = "</pre>";
  public String [] textPublishers;
  public String [] textListeners;

}
