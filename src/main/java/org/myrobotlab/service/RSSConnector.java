package org.myrobotlab.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.service.config.RSSConnectorConfig;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RSSConnector extends AbstractConnector<RSSConnectorConfig>  {

  private static final long serialVersionUID = 1L;

  private String rssUrl = "http://www.myrobotlab.org/rss.xml";
  private boolean interrupted = false;

  public RSSConnector(String n, String id) {
    super(n, id);
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

    SyndFeedInput input = new SyndFeedInput();
    SyndFeed feed;
    try {
      feed = input.build(new XmlReader(url));
    } catch (IllegalArgumentException | FeedException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      this.state = ConnectorState.STOPPED;
      return;
    }

    // System.out.println(feed);
    // try {
    // feed = FeedParser.parse(url);
    // } catch (FeedIOException | FeedXMLParseException |
    // UnsupportedFeedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // return;
    // }

    // FeedHeader header = feed.getHeader();
    // int items = feed.getItemCount();

    int i = 0;
    for (Object o : feed.getEntries()) {

      SyndEntryImpl item = (SyndEntryImpl) o;

      i++;
      if (interrupted) {
        state = ConnectorState.INTERRUPTED;
        // TODO: clean up after yourself!
        return;
      }
      // FeedItem item = feed.getItem(i);
      // create an id for this as being url # item offset
      Document feedItem = new Document(url + "#" + i);
      feedItem.setField("rss_title", feed.getTitle());
      feedItem.setField("rss_link", feed.getLink());
      feedItem.setField("rss_description", feed.getDescription());
      feedItem.setField("rss_language", feed.getLanguage());
      feedItem.setField("rss_date", feed.getPublishedDate());
      feedItem.setField("title", item.getTitle());
      feedItem.setField("link", item.getLink());
      // TODO: if this is html vs plain text. we should
      String text = HtmlFilter.stripHtml(item.getDescription().getValue());
      feedItem.setField("description", text);
      feedItem.setField("date", item.getPublishedDate());
      feedItem.setField("html", item.getDescription().getValue());
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
    // Solr solr = (Solr) Runtime.start("solr", "Solr");
    // solr.setSolrUrl("http://www.skizatch.org:8983/solr/graph");
    // connector.addDocumentListener(solr);
    connector.startCrawling();
  }

  @Override
  public void stopCrawling() {
    // interrupt the current crawl gently.
    interrupted = true;
  }

}
