package org.myrobotlab.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.framework.ServiceType;

import it.sauronsoftware.feed4j.FeedIOException;
import it.sauronsoftware.feed4j.FeedParser;
import it.sauronsoftware.feed4j.FeedXMLParseException;
import it.sauronsoftware.feed4j.UnsupportedFeedException;
import it.sauronsoftware.feed4j.bean.Feed;
import it.sauronsoftware.feed4j.bean.FeedHeader;
import it.sauronsoftware.feed4j.bean.FeedItem;

public class RSSConnector extends AbstractConnector {

  private static final long serialVersionUID = 1L;

  private String rssUrl = "http://www.myrobotlab.org/rss.xml";
  private boolean interrupted = false;

  public RSSConnector(String reservedKey) {
    super(reservedKey);
  }

  @Override
  public void setConfig(ConnectorConfig config) {
    // TODO Auto-generated method stub
    log.info("Set Config not yet implemented");
  }

  @Override
  public void startCrawling() {
    // TODO : make this cooler. :) for now.. fire and forget
    this.state = ConnectorState.RUNNING;
    URL url;
    try {
      url = new URL(rssUrl);
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }
    Feed feed;
    try {
      feed = FeedParser.parse(url);
    } catch (FeedIOException | FeedXMLParseException | UnsupportedFeedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }

    FeedHeader header = feed.getHeader();
    int items = feed.getItemCount();
    for (int i = 0; i < items; i++) {
      if (interrupted) {
        state = ConnectorState.INTERRUPTED;
        // TODO: clean up after yourself!
        return;
      }
      FeedItem item = feed.getItem(i);
      // create an id for this as being url # item offset
      Document feedItem = new Document(url + "#" + i);
      feedItem.setField("rss_title", header.getTitle());
      feedItem.setField("rss_link", header.getLink());
      feedItem.setField("rss_description", header.getDescription());
      feedItem.setField("rss_language", header.getLanguage());
      feedItem.setField("rss_date", header.getPubDate());
      feedItem.setField("title", item.getTitle());
      feedItem.setField("link", item.getLink());
      feedItem.setField("description", item.getDescriptionAsText());
      feedItem.setField("date", item.getPubDate());
      feedItem.setField("html", item.getDescriptionAsHTML());
      feed(feedItem);
    }
    // flush the last partial batch of documents if we are batching.
    flush();
    this.state = ConnectorState.STOPPED;
  }

  public String getRssUrl() {
    return rssUrl;
  }

  public void setRssUrl(String rssUrl) {
    this.rssUrl = rssUrl;
  }

  public static void main(String[] args) throws Exception {
    RSSConnector connector = (RSSConnector) Runtime.start("rss", "RSSConnector");
    Solr solr = (Solr) Runtime.start("solr", "Solr");
    solr.setSolrUrl("http://www.skizatch.org:8983/solr/graph");
    connector.addDocumentListener(solr);
    connector.startCrawling();
  }

  @Override
  public void stopCrawling() {
    // interrupt the current crawl gently.
    interrupted = true;
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

    ServiceType meta = new ServiceType(RSSConnector.class.getCanonicalName());
    meta.addDescription("This will crawl an rss feed at the given url and break apart the page into Documents");
    meta.setCloudService(true);
    meta.addCategory("data");
    // Tried to add this dependency, but no luck with defining the ivy.xml
    // meta.addDependency("feed4j", "1.0");

    return meta;
  }

}
